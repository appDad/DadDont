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
import com.egabel.daddont.data.model.CoolingConfig
import com.egabel.daddont.data.model.ImpulseKind
import com.egabel.daddont.data.model.Tier
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.widget.WidgetUpdater
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

        val breachContext = repository.breachSummary()
        var failures = 0
        for (impulse in ungraded) {
            try {
                val c = gemini.classify(impulse.content, breachContext)
                if (c == null) {
                    failures++
                    continue
                }
                val kind = runCatching { ImpulseKind.valueOf(c.kind) }
                    .getOrDefault(ImpulseKind.DECISION)
                val classified = repository.applyClassification(
                    impulse.copy(
                        kind = kind,
                        tier = Tier.valueOf(c.tier),
                        category = Category.valueOf(c.category),
                        partnerGate = c.partnerGate,
                        partnerReason = c.partnerReason.ifEmpty { null },
                        trigger = c.trigger ?: impulse.trigger,
                        rationale = c.rationale ?: impulse.rationale,
                        estimatedCost = impulse.estimatedCost ?: c.estimatedCostUsd,
                        desireAtCapture = impulse.desireAtCapture ?: c.desireStrength,
                        decideBy = if (kind == ImpulseKind.HOLD) {
                            c.holdUntilMs ?: CoolingConfig.defaultHoldEnd()
                        } else impulse.decideBy
                    )
                )
                // Seed the desire curve if the user never set a slider value
                if (impulse.desireAtCapture == null && c.desireStrength != null) {
                    repository.addDesireCheckIn(impulse.id, c.desireStrength)
                }
                classified.decideBy?.let {
                    VerdictWorker.schedule(applicationContext, classified.id, it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to classify impulse ${impulse.id}", e)
                failures++
            }
        }

        WidgetUpdater.updateAll(applicationContext)
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
