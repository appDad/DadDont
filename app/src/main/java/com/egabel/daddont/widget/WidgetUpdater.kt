package com.egabel.daddont.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.R
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.ImpulseStateCalculator
import com.egabel.daddont.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Renders the 1x1 home-screen beacon. Priority of what it shows:
 *
 *   GREEN  + count — verdicts due. The number is decisions waiting on you.
 *   RED    + count — hot impulses cooling. The number is active impulses.
 *   AMBER  + count — everything past the hot phase, still cooling.
 *   BLUE   + count — captures awaiting classification.
 *   GRAY, no count — all clear.
 */
object WidgetUpdater {

    // Matches ui/theme/Color.kt state palette
    private const val COLOR_GREEN = 0xFF2FA36C.toInt()
    private const val COLOR_RED = 0xFFE05C5C.toInt()
    private const val COLOR_AMBER = 0xFFE59933.toInt()
    private const val COLOR_BLUE = 0xFF5B8DEF.toInt()
    private const val COLOR_GRAY = 0xFF9EA8BE.toInt()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val manager = AppWidgetManager.getInstance(appContext)
                val ids = manager.getAppWidgetIds(
                    ComponentName(appContext, StatusWidgetProvider::class.java)
                )
                if (ids.isEmpty()) return@launch

                val db = (appContext as DadDontApp).database
                val active = db.impulseDao().getAll().filter { it.verdict == null }
                val views = render(appContext, active)
                ids.forEach { id -> manager.updateAppWidget(id, views) }

                scheduleNextRefresh(appContext, active)
            } catch (_: Exception) {
                // Widget updates must never crash the caller
            }
        }
    }

    private fun render(context: Context, active: List<Impulse>): RemoteViews {
        val now = System.currentTimeMillis()
        val byState = active.groupBy { ImpulseStateCalculator.computeState(it, now) }

        val decide = byState[ImpulseState.GREEN]?.size ?: 0
        val hot = byState[ImpulseState.RED]?.size ?: 0
        val cooling = byState[ImpulseState.YELLOW]?.size ?: 0
        val pending = byState[ImpulseState.PENDING]?.size ?: 0

        val (color, count, label) = when {
            decide > 0 -> Triple(COLOR_GREEN, decide, "DECIDE")
            hot > 0 -> Triple(COLOR_RED, hot + cooling, "HOT")
            cooling > 0 -> Triple(COLOR_AMBER, cooling, "COOLING")
            pending > 0 -> Triple(COLOR_BLUE, pending, "NEW")
            else -> Triple(COLOR_GRAY, 0, "CLEAR")
        }

        return RemoteViews(context.packageName, R.layout.widget_status).apply {
            setInt(R.id.widget_bg, "setColorFilter", color)
            setTextViewText(R.id.widget_count, if (count > 0) "$count" else "✓")
            setTextViewText(R.id.widget_label, label)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    /** Keep the color honest: refresh again at the earliest upcoming transition. */
    private fun scheduleNextRefresh(context: Context, active: List<Impulse>) {
        val now = System.currentTimeMillis()
        val nextTransition = active
            .mapNotNull { ImpulseStateCalculator.msUntilNextState(it, now) }
            .minOrNull() ?: return
        WidgetRefreshWorker.schedule(context, nextTransition)
    }
}
