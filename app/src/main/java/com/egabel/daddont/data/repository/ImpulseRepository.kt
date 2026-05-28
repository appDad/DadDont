package com.egabel.daddont.data.repository

import com.egabel.daddont.data.db.DadDontDatabase
import com.egabel.daddont.data.model.DialogSession
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.ImpulseStateCalculator
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

data class ImpulseWithState(
    val impulse: Impulse,
    val state: ImpulseState,
    val msUntilNext: Long? = null
)

class ImpulseRepository(private val db: DadDontDatabase) {
    private val impulseDao = db.impulseDao()
    private val returnEventDao = db.returnEventDao()
    private val dialogSessionDao = db.dialogSessionDao()

    suspend fun capture(content: String): Impulse {
        val impulse = Impulse(content = content)
        impulseDao.insert(impulse)
        return impulse
    }

    fun observeActiveWithState(): Flow<List<ImpulseWithState>> =
        impulseDao.observeActive().map { impulses ->
            impulses.map {
                ImpulseWithState(it, ImpulseStateCalculator.computeState(it),
                    ImpulseStateCalculator.msUntilNextState(it))
            }
        }

    fun observeArchivedWithState(): Flow<List<ImpulseWithState>> =
        impulseDao.observeArchived().map { impulses ->
            impulses.map { ImpulseWithState(it, ImpulseState.GRAY) }
        }

    fun observePartnerFlagged(): Flow<List<ImpulseWithState>> =
        impulseDao.observePartnerFlagged().map { impulses ->
            impulses.map {
                ImpulseWithState(it, ImpulseStateCalculator.computeState(it),
                    ImpulseStateCalculator.msUntilNextState(it))
            }
        }

    fun observeById(id: UUID): Flow<ImpulseWithState?> =
        impulseDao.observeById(id).map { impulse ->
            impulse?.let {
                ImpulseWithState(it, ImpulseStateCalculator.computeState(it),
                    ImpulseStateCalculator.msUntilNextState(it))
            }
        }

    suspend fun getById(id: UUID): ImpulseWithState? {
        val impulse = impulseDao.getById(id) ?: return null
        return ImpulseWithState(impulse, ImpulseStateCalculator.computeState(impulse),
            ImpulseStateCalculator.msUntilNextState(impulse))
    }

    suspend fun recordReturn(impulseId: UUID, rationale: String? = null) {
        val impulse = impulseDao.getById(impulseId) ?: return
        val state = ImpulseStateCalculator.computeState(impulse)

        if (state == ImpulseState.GRAY) {
            // Reactivation: reset cooling clock
            val now = System.currentTimeMillis()
            impulseDao.update(impulse.copy(
                classifiedAt = now,
                reactivationCount = impulse.reactivationCount + 1,
                dismissedAt = null,
                dismissalType = null
            ))
        } else {
            impulseDao.update(impulse.copy(returnCount = impulse.returnCount + 1))
        }

        returnEventDao.insert(ReturnEvent(impulseId = impulseId, rationale = rationale))
    }

    suspend fun dismiss(impulseId: UUID, type: DismissalType) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(
            dismissedAt = System.currentTimeMillis(),
            dismissalType = type
        ))
    }

    suspend fun markSentToDadDo(impulseId: UUID) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(sentToDadDoAt = System.currentTimeMillis()))
    }

    suspend fun togglePartnerFlag(impulseId: UUID) {
        val impulse = impulseDao.getById(impulseId) ?: return
        impulseDao.update(impulse.copy(partnerGate = !impulse.partnerGate))
    }

    suspend fun getUngraded(): List<Impulse> = impulseDao.getUngraded()

    suspend fun updateClassification(impulse: Impulse) {
        impulseDao.update(impulse)
    }

    fun observeReturnEvents(impulseId: UUID): Flow<List<ReturnEvent>> =
        returnEventDao.observeForImpulse(impulseId)

    suspend fun getReturnEvents(impulseId: UUID): List<ReturnEvent> =
        returnEventDao.getForImpulse(impulseId)

    suspend fun saveDialogSession(impulseId: UUID, transcript: String) {
        dialogSessionDao.insert(DialogSession(impulseId = impulseId, transcript = transcript))
    }

    fun observeDialogSessions(impulseId: UUID): Flow<List<DialogSession>> =
        dialogSessionDao.observeForImpulse(impulseId)

    suspend fun getLatestDialogTranscript(impulseId: UUID): String? =
        dialogSessionDao.getLatestForImpulse(impulseId)?.transcript

    // Stats
    suspend fun countDismissedSince(since: Long): Int = impulseDao.countDismissedSince(since)
    suspend fun countExecutedSince(since: Long): Int = impulseDao.countExecutedSince(since)
    suspend fun countActive(): Int = impulseDao.countActive()
    suspend fun topRecurrenceOffenders(limit: Int = 10): List<Impulse> =
        impulseDao.topRecurrenceOffenders(limit)
}
