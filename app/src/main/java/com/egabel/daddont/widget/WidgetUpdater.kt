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
        val allClear = decide + hot + cooling + pending == 0

        return RemoteViews(context.packageName, R.layout.widget_status).apply {
            // One cell per state; zero-count cells collapse and the rest grow
            fun cell(cellId: Int, bgId: Int, countId: Int, color: Int, count: Int) {
                if (count > 0) {
                    setViewVisibility(cellId, android.view.View.VISIBLE)
                    setInt(bgId, "setColorFilter", color)
                    setTextViewText(countId, "$count")
                } else {
                    setViewVisibility(cellId, android.view.View.GONE)
                }
            }
            cell(R.id.cell_green, R.id.cell_green_bg, R.id.cell_green_count, COLOR_GREEN, decide)
            cell(R.id.cell_red, R.id.cell_red_bg, R.id.cell_red_count, COLOR_RED, hot)
            cell(R.id.cell_amber, R.id.cell_amber_bg, R.id.cell_amber_count, COLOR_AMBER, cooling)
            cell(R.id.cell_blue, R.id.cell_blue_bg, R.id.cell_blue_count, COLOR_BLUE, pending)

            // Collapse empty rows so the other row fills the square
            setViewVisibility(
                R.id.row_top,
                if (decide > 0 || hot > 0) android.view.View.VISIBLE else android.view.View.GONE
            )
            setViewVisibility(
                R.id.row_bottom,
                if (cooling > 0 || pending > 0) android.view.View.VISIBLE else android.view.View.GONE
            )

            // Backdrop: gray card when all clear, white behind the mosaic
            setInt(R.id.widget_bg, "setColorFilter", if (allClear) COLOR_GRAY else 0xFFFFFFFF.toInt())
            setViewVisibility(
                R.id.widget_clear,
                if (allClear) android.view.View.VISIBLE else android.view.View.GONE
            )
            setViewVisibility(
                R.id.widget_grid,
                if (allClear) android.view.View.GONE else android.view.View.VISIBLE
            )

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
