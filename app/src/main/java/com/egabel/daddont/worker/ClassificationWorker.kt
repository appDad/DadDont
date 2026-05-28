package com.egabel.daddont.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.api.gemini.GeminiClient
import com.egabel.daddont.data.model.Category
import com.egabel.daddont.data.model.Tier
import com.egabel.daddont.data.repository.ImpulseRepository
import java.util.concurrent.TimeUnit

class ClassificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = (applicationContext as DadDontApp).database
        val repository = ImpulseRepository(db)
        val gemini = GeminiClient(applicationContext)

        val ungraded = repository.getUngraded()
        if (ungraded.isEmpty()) return Result.success()

        var failures = 0
        for (impulse in ungraded) {
            try {
                val classification = gemini.classify(impulse.content)
                if (classification == null) {
                    failures++
                    continue
                }
                repository.updateClassification(
                    impulse.copy(
                        tier = Tier.valueOf(classification.tier),
                        category = Category.valueOf(classification.category),
                        ramonaGate = classification.ramonaGate,
                        ramonaReason = classification.ramonaReason.ifEmpty { null },
                        ungraded = false
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to classify impulse ${impulse.id}", e)
                failures++
            }
        }

        return if (failures == ungraded.size) Result.retry() else Result.success()
    }

    companion object {
        private const val TAG = "ClassificationWorker"
        private const val WORK_NAME = "impulse_classification"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ClassificationWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
