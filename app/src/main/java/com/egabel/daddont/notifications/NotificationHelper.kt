package com.egabel.daddont.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.egabel.daddont.R
import com.egabel.daddont.ui.MainActivity
import java.util.UUID

object NotificationHelper {
    const val CHANNEL_VERDICTS = "verdicts"
    const val EXTRA_IMPULSE_ID = "impulseId"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_VERDICTS,
                "Verdicts due",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "An impulse has cooled and needs your decision"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun notifyVerdictDue(
        context: Context,
        impulseId: UUID,
        content: String,
        isHold: Boolean = false
    ) {
        if (!canNotify(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_IMPULSE_ID, impulseId.toString())
        }
        val pending = PendingIntent.getActivity(
            context,
            impulseId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isHold) "You made it — it's open" else "Cooled — verdict time"
        val bigText = if (isHold) {
            "You held out on \"$content\". It's allowed now — close it out."
        } else {
            "\"$content\" has finished cooling. Did it, or kill it?"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_VERDICTS)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context)
            .notify(impulseId.hashCode(), notification)
    }

    fun notifyDigest(context: Context, overdueCount: Int) {
        if (!canNotify(context) || overdueCount == 0) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val plural = if (overdueCount == 1) "decision" else "decisions"
        val notification = NotificationCompat.Builder(context, CHANNEL_VERDICTS)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("$overdueCount overdue $plural")
            .setContentText("Impulses are cooled and waiting on your verdict.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(-1, notification)
    }
}
