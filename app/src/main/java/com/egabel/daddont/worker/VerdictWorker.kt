package com.egabel.daddont.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.ImpulseStateCalculator
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.notifications.NotificationHelper
import com.egabel.daddont.widget.WidgetUpdater
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Fires when an impulse's decideBy moment arrives and posts the
 * "verdict time" notification. Scheduled per-impulse, replaced whenever
 * the deadline moves (classification, defer, custom cool-until).
 */
class VerdictWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val idStr = inputData.getString(KEY_IMPULSE_ID) ?: return Result.success()
        val impulseId = runCatching { UUID.fromString(idStr) }.getOrNull() ?: return Result.success()

        val db = (applicationContext as DadDontApp).database
        val repository = ImpulseRepository(db)
        val impulse = repository.getById(impulseId)?.impulse ?: return Result.success()

        // Only notify if it's actually awaiting a verdict (deadline may have moved)
        val state = ImpulseStateCalculator.computeState(impulse)
        if (state == ImpulseState.GREEN) {
            NotificationHelper.notifyVerdictDue(applicationContext, impulse.id, impulse.content)
        }
        WidgetUpdater.updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val KEY_IMPULSE_ID = "impulseId"

        fun schedule(context: Context, impulseId: UUID, decideBy: Long) {
            val delay = (decideBy - System.currentTimeMillis()).coerceAtLeast(0)
            val request = OneTimeWorkRequestBuilder<VerdictWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(KEY_IMPULSE_ID, impulseId.toString()).build())
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(impulseId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, impulseId: UUID) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(impulseId))
        }

        private fun workName(impulseId: UUID) = "verdict_$impulseId"
    }
}
