package com.egabel.daddont.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.notifications.NotificationHelper
import com.egabel.daddont.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Daily accountability nudge: if any impulses are cooled and awaiting
 * a verdict, post a summary. Ignoring a decision doesn't make it
 * disappear — it makes it louder.
 */
class DigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = (applicationContext as DadDontApp).database
        val repository = ImpulseRepository(db)

        val awaiting = repository.getAwaitingVerdict()
        NotificationHelper.notifyDigest(applicationContext, awaiting.size)
        WidgetUpdater.updateAll(applicationContext)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_digest"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DigestWorker>(
                24, TimeUnit.HOURS
            ).setInitialDelay(12, TimeUnit.HOURS).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
