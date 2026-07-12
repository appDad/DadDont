package com.egabel.daddont.data.repository

import com.egabel.daddont.data.db.DadDontDatabase
import com.egabel.daddont.data.model.BreachEvent
import com.egabel.daddont.data.model.CoolingConfig
import com.egabel.daddont.data.model.DesireCheckIn
import com.egabel.daddont.data.model.DialogSession
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.model.ImpulseKind
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.ImpulseStateCalculator
import com.egabel.daddont.data.model.Prediction
import com.egabel.daddont.data.model.ReturnEvent
import com.egabel.daddont.data.model.Verdict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ImpulseWithState(
    val impulse: Impulse,
    val state: ImpulseState,
    val msUntilNext: Long? = null,
    val overdueMs: Long? = null
)

/** Everything the scorecard needs, computed from raw rows. */
data class Scorecard(
    val killedCount: Int,
    val didItCount: Int,
    val moneyNotSpent: Double,
    val awaitingVerdict: Int,
    val overdueCount: Int,
    val predictionsMade: Int,
    val predictionsCorrect: Int,
    val avgDesireDecay: Double?, // avg(first - latest) across impulses with 2+ check-ins
    val topOffenders: List<Impulse>,
    val brokeCount: Int,             // gave-up verdicts in the window
    val slipCount: Int,              // slips (kept going) in the window
    val heldCount: Int,              // holds survived to their end time in the window
    val moneySpentBreaking: Double,  // cost of gave-up purchases in the window
    val cleanStreakDays: Int?        // days since last slip or give-up ever; null = clean record
)

class ImpulseRepository(private val db: DadDontDatabase) {
    private val impulseDao = db.impulseDao()
    private val returnEventDao = db.returnEventDao()
    private val dialogSessionDao = db.dialogSessionDao()
    private val desireCheckInDao = db.desireCheckInDao()
    private val breachEventDao = db.breachEventDao()

    // ── Capture ──────────────────────────────────────────────────────

    suspend fun capture(content: String): Impulse {
        val impulse = Impulse(content = content)
        impulseDao.insert(impulse)
        return impulse
    }

    /** Quick-facts sheet: desire strength, cost, prediction. */
    suspend fun updateFacets(
        impulseId: UUID,
        desire: Int?,
        estimatedCost: Double?,
        prediction: Prediction?
    ) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(
            desireAtCapture = desire ?: impulse.desireAtCapture,
            estimatedCost = estimatedCost ?: impulse.estimatedCost,
            prediction = prediction ?: impulse.prediction
        ))
        if (desire != null) {
            desireCheckInDao.insert(DesireCheckIn(impulseId = impulseId, strength = desire))
        }
    }

    /** Called when classification completes. Sets the decision contract. */
    suspend fun applyClassification(impulse: Impulse): Impulse {
        val now = System.currentTimeMillis()
        val tier = impulse.tier
        val updated = impulse.copy(
            classifiedAt = impulse.classifiedAt ?: now,
            decideBy = impulse.decideBy
                ?: tier?.let { (impulse.classifiedAt ?: now) + CoolingConfig.windowFor(it) },
            ungraded = false
        )
        impulseDao.update(updated)
        return updated
    }

    // ── Observation ──────────────────────────────────────────────────

    private fun Impulse.withState(): ImpulseWithState {
        val now = System.currentTimeMillis()
        return ImpulseWithState(
            impulse = this,
            state = ImpulseStateCalculator.computeState(this, now),
            msUntilNext = ImpulseStateCalculator.msUntilNextState(this, now),
            overdueMs = ImpulseStateCalculator.overdueMs(this, now)
        )
    }

    fun observeActiveWithState(): Flow<List<ImpulseWithState>> =
        impulseDao.observeActive().map { impulses -> impulses.map { it.withState() } }

    fun observeArchivedWithState(): Flow<List<ImpulseWithState>> =
        impulseDao.observeArchived().map { impulses -> impulses.map { it.withState() } }

    fun observePartnerFlagged(): Flow<List<ImpulseWithState>> =
        impulseDao.observePartnerFlagged().map { impulses -> impulses.map { it.withState() } }

    fun observeById(id: UUID): Flow<ImpulseWithState?> =
        impulseDao.observeById(id).map { it?.withState() }

    suspend fun getById(id: UUID): ImpulseWithState? =
        impulseDao.getById(id)?.withState()

    // ── Verdicts — the only exit from the active list ────────────────

    suspend fun recordVerdict(impulseId: UUID, verdict: Verdict, note: String? = null) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(
            verdict = verdict,
            verdictAt = System.currentTimeMillis(),
            verdictNote = note
        ))
    }

    /** Defer: not ready to decide — re-cool to a new deadline, reason required. */
    suspend fun defer(impulseId: UUID, newDecideBy: Long, reason: String) {
        val impulse = impulseDao.getById(impulseId) ?: return
        val now = System.currentTimeMillis()
        impulseDao.update(impulse.copy(
            classifiedAt = now,
            decideBy = newDecideBy,
            deferCount = impulse.deferCount + 1
        ))
        returnEventDao.insert(ReturnEvent(
            impulseId = impulseId,
            rationale = "Deferred: $reason"
        ))
    }

    /** Reactivate an archived impulse: clear verdict, start a fresh window. */
    suspend fun reactivate(impulseId: UUID) {
        val impulse = impulseDao.getById(impulseId) ?: return
        val now = System.currentTimeMillis()
        val window = impulse.tier?.let { CoolingConfig.windowFor(it) } ?: CoolingConfig.mediumWindowMs
        impulseDao.update(impulse.copy(
            verdict = null,
            verdictAt = null,
            verdictNote = null,
            classifiedAt = now,
            decideBy = now + window,
            reactivationCount = impulse.reactivationCount + 1
        ))
    }

    // ── Returns & desire check-ins ───────────────────────────────────

    suspend fun recordReturn(impulseId: UUID, rationale: String? = null, desireNow: Int? = null) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(returnCount = impulse.returnCount + 1))
        returnEventDao.insert(ReturnEvent(impulseId = impulseId, rationale = rationale))
        if (desireNow != null) {
            desireCheckInDao.insert(DesireCheckIn(impulseId = impulseId, strength = desireNow))
        }
    }

    suspend fun addDesireCheckIn(impulseId: UUID, strength: Int) {
        desireCheckInDao.insert(DesireCheckIn(impulseId = impulseId, strength = strength))
    }

    fun observeDesireCheckIns(impulseId: UUID): Flow<List<DesireCheckIn>> =
        desireCheckInDao.observeForImpulse(impulseId)

    suspend fun getDesireCheckIns(impulseId: UUID): List<DesireCheckIn> =
        desireCheckInDao.getForImpulse(impulseId)

    // ── Editing ──────────────────────────────────────────────────────

    suspend fun updateContent(impulseId: UUID, newContent: String) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(content = newContent))
    }

    suspend fun setDecideBy(impulseId: UUID, decideByMs: Long) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(decideBy = decideByMs))
    }

    suspend fun togglePartnerFlag(impulseId: UUID) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(partnerGate = !impulse.partnerGate))
    }

    suspend fun delete(impulseId: UUID) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.delete(impulse)
    }

    suspend fun getUngraded(): List<Impulse> = impulseDao.getUngraded()

    suspend fun updateClassification(impulse: Impulse) {
        impulseDao.update(impulse)
    }

    // ── Return events & dialog sessions ──────────────────────────────

    fun observeReturnEvents(impulseId: UUID): Flow<List<ReturnEvent>> =
        returnEventDao.observeForImpulse(impulseId)

    suspend fun getReturnEvents(impulseId: UUID): List<ReturnEvent> =
        returnEventDao.getForImpulse(impulseId)

    suspend fun saveDialogSession(impulseId: UUID, transcript: String) {
        dialogSessionDao.insert(DialogSession(impulseId = impulseId, transcript = transcript))
    }

    suspend fun getLatestDialogTranscript(impulseId: UUID): String? =
        dialogSessionDao.getLatestForImpulse(impulseId)?.transcript

    // ── Notifications support ────────────────────────────────────────

    suspend fun getAwaitingVerdict(): List<Impulse> =
        impulseDao.getAwaitingVerdict(System.currentTimeMillis())

    // ── Breach reinforcement ─────────────────────────────────────────

    /**
     * A slip: acted early but the impulse continues. The impulse stays
     * active — recording and continuing is the reinforcement.
     */
    suspend fun recordSlip(impulseId: UUID, note: String) {
        breachEventDao.insert(BreachEvent(impulseId = impulseId, note = note))
    }

    fun observeBreachEvents(impulseId: UUID): Flow<List<BreachEvent>> =
        breachEventDao.observeForImpulse(impulseId)

    /**
     * Short human-readable summary of recent slips and give-ups, fed to
     * Gemini so classification cools lookalike impulses longer and Talk Me
     * Down can cite the pattern. Null when the record is clean.
     */
    suspend fun breachSummary(limit: Int = 5): String? {
        val all = impulseDao.getAll()
        val byId = all.associateBy { it.id }

        val gaveUp = all
            .filter { it.verdict == Verdict.BROKE }
            .map { b ->
                val cost = b.estimatedCost?.let { " ($${"%,.0f".format(it)})" } ?: ""
                val why = b.verdictNote?.let { " — \"$it\"" } ?: ""
                Triple(b.verdictAt ?: 0, "gave up", "[${b.category?.name ?: "OTHER"}] \"${b.content}\"$cost$why")
            }

        val slips = breachEventDao.getAll().mapNotNull { e ->
            val impulse = byId[e.impulseId] ?: return@mapNotNull null
            Triple(e.timestamp, "slipped", "[${impulse.category?.name ?: "OTHER"}] \"${impulse.content}\" — \"${e.note}\"")
        }

        val combined = (gaveUp + slips).sortedByDescending { it.first }.take(limit)
        if (combined.isEmpty()) return null

        return combined.joinToString("\n") { (_, kind, line) -> "- ($kind) $line" }
    }

    // ── Scorecard ────────────────────────────────────────────────────

    suspend fun computeScorecard(since: Long): Scorecard {
        val now = System.currentTimeMillis()
        val all = impulseDao.getAll()
        val checkIns = desireCheckInDao.getAll().groupBy { it.impulseId }

        val recentVerdicts = all.filter { it.verdict != null && (it.verdictAt ?: 0) >= since }
        val killed = recentVerdicts.filter { it.verdict == Verdict.KILLED }
        val didIt = recentVerdicts.filter { it.verdict == Verdict.DID_IT }
        val broke = recentVerdicts.filter { it.verdict == Verdict.BROKE }
        val held = recentVerdicts.filter { it.verdict == Verdict.HELD }

        val moneyNotSpent = killed.sumOf { it.estimatedCost ?: 0.0 } +
            held.sumOf { it.estimatedCost ?: 0.0 }
        val moneySpentBreaking = broke.sumOf { it.estimatedCost ?: 0.0 }

        val slipCount = breachEventDao.countSince(since)

        // Streak resets on ANY breach — slip or give-up
        val lastGiveUp = all
            .filter { it.verdict == Verdict.BROKE }
            .maxOfOrNull { it.verdictAt ?: 0 }
        val lastSlip = breachEventDao.lastSlipAt()
        val lastBreachEver = listOfNotNull(lastGiveUp, lastSlip).maxOrNull()
        val cleanStreakDays = lastBreachEver?.let {
            ((now - it) / 86_400_000L).toInt()
        }

        val active = all.filter { it.verdict == null }
        val awaiting = active.filter { it.decideBy != null && now >= it.decideBy!! }
        // "Overdue" = waiting on a verdict for more than a day
        val overdue = awaiting.filter { now - it.decideBy!! > 86_400_000L }

        // Breaches are a discipline failure, not a forecasting one — exclude them
        val withPrediction = recentVerdicts.filter {
            it.prediction != null && it.verdict != Verdict.BROKE
        }
        val correct = withPrediction.count {
            (it.prediction == Prediction.STILL_WANT && it.verdict == Verdict.DID_IT) ||
            (it.prediction == Prediction.MOVED_ON && it.verdict == Verdict.KILLED)
        }

        val decays = all.mapNotNull { impulse ->
            val curve = checkIns[impulse.id]?.sortedBy { it.timestamp } ?: return@mapNotNull null
            if (curve.size < 2) return@mapNotNull null
            (curve.first().strength - curve.last().strength).toDouble()
        }

        return Scorecard(
            killedCount = killed.size,
            didItCount = didIt.size,
            moneyNotSpent = moneyNotSpent,
            awaitingVerdict = awaiting.size,
            overdueCount = overdue.size,
            predictionsMade = withPrediction.size,
            predictionsCorrect = correct,
            avgDesireDecay = decays.takeIf { it.isNotEmpty() }?.average(),
            topOffenders = impulseDao.topRecurrenceOffenders(5),
            brokeCount = broke.size,
            slipCount = slipCount,
            heldCount = held.size,
            moneySpentBreaking = moneySpentBreaking,
            cleanStreakDays = cleanStreakDays
        )
    }
}
