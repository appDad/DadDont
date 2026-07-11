package com.egabel.daddont.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.api.gemini.TalkMeDownClient
import com.egabel.daddont.data.model.Category
import com.egabel.daddont.data.model.DesireCheckIn
import com.egabel.daddont.data.model.ReturnEvent
import com.egabel.daddont.data.model.Tier
import com.egabel.daddont.data.model.Verdict
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.data.repository.ImpulseWithState
import com.egabel.daddont.widget.WidgetUpdater
import com.egabel.daddont.worker.VerdictWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class DialogMessage(val role: String, val content: String)

data class ImpulseDetailUiState(
    val impulseWithState: ImpulseWithState? = null,
    val returnEvents: List<ReturnEvent> = emptyList(),
    val desireCurve: List<DesireCheckIn> = emptyList(),
    val dialogMessages: List<DialogMessage> = emptyList(),
    val isDialogLoading: Boolean = false,
    val dialogInput: String = "",
    val showTalkMeDown: Boolean = false,
    val error: String? = null
)

class ImpulseDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = ImpulseRepository((application as DadDontApp).database)
    private val talkMeDownClient = TalkMeDownClient(application)

    private val impulseId: UUID = UUID.fromString(savedStateHandle.get<String>("impulseId")!!)

    private val _dialogMessages = MutableStateFlow<List<DialogMessage>>(emptyList())
    private val _isDialogLoading = MutableStateFlow(false)
    private val _dialogInput = MutableStateFlow("")
    private val _showTalkMeDown = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val impulseFlow = repository.observeById(impulseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private val returnEventsFlow = repository.observeReturnEvents(impulseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val desireCurveFlow = repository.observeDesireCheckIns(impulseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ImpulseDetailUiState> = combine(
        impulseFlow, returnEventsFlow, desireCurveFlow, _dialogMessages,
        _isDialogLoading, _dialogInput, _showTalkMeDown, _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ImpulseDetailUiState(
            impulseWithState = values[0] as ImpulseWithState?,
            returnEvents = values[1] as List<ReturnEvent>,
            desireCurve = values[2] as List<DesireCheckIn>,
            dialogMessages = values[3] as List<DialogMessage>,
            isDialogLoading = values[4] as Boolean,
            dialogInput = values[5] as String,
            showTalkMeDown = values[6] as Boolean,
            error = values[7] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImpulseDetailUiState())

    fun setDialogInput(text: String) { _dialogInput.value = text }
    fun toggleTalkMeDown() { _showTalkMeDown.value = !_showTalkMeDown.value }
    fun clearError() { _error.value = null }

    // ── Verdicts ─────────────────────────────────────────────────────

    fun recordVerdict(verdict: Verdict, note: String? = null) {
        viewModelScope.launch {
            repository.recordVerdict(impulseId, verdict, note)
            VerdictWorker.cancel(getApplication(), impulseId)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun defer(newDecideBy: Long, reason: String) {
        viewModelScope.launch {
            repository.defer(impulseId, newDecideBy, reason)
            VerdictWorker.schedule(getApplication(), impulseId, newDecideBy)
            WidgetUpdater.updateAll(getApplication())
        }
    }

    fun reactivate() {
        viewModelScope.launch {
            repository.reactivate(impulseId)
            repository.getById(impulseId)?.impulse?.decideBy?.let {
                VerdictWorker.schedule(getApplication(), impulseId, it)
            }
            WidgetUpdater.updateAll(getApplication())
        }
    }

    // ── Returns & desire ─────────────────────────────────────────────

    fun recordReturn(rationale: String? = null, desireNow: Int? = null) {
        viewModelScope.launch {
            repository.recordReturn(impulseId, rationale, desireNow)
        }
    }

    fun checkInDesire(strength: Int) {
        viewModelScope.launch {
            repository.addDesireCheckIn(impulseId, strength)
        }
    }

    // ── Editing ──────────────────────────────────────────────────────

    fun togglePartnerFlag() {
        viewModelScope.launch {
            repository.togglePartnerFlag(impulseId)
        }
    }

    fun updateContent(newContent: String) {
        viewModelScope.launch {
            repository.updateContent(impulseId, newContent)
        }
    }

    fun setDecideBy(decideByMs: Long) {
        viewModelScope.launch {
            repository.setDecideBy(impulseId, decideByMs)
            VerdictWorker.schedule(getApplication(), impulseId, decideByMs)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.delete(impulseId)
            VerdictWorker.cancel(getApplication(), impulseId)
            WidgetUpdater.updateAll(getApplication())
            onDeleted()
        }
    }

    // ── Talk Me Down ─────────────────────────────────────────────────

    fun sendDialogMessage() {
        val text = _dialogInput.value.trim()
        if (text.isEmpty()) return
        _dialogInput.value = ""
        _dialogMessages.value = _dialogMessages.value + DialogMessage("user", text)
        _isDialogLoading.value = true

        viewModelScope.launch {
            try {
                val impulse = repository.getById(impulseId)?.impulse ?: return@launch
                val returnEvents = repository.getReturnEvents(impulseId)
                val desireCurve = repository.getDesireCheckIns(impulseId)
                val priorTranscript = repository.getLatestDialogTranscript(impulseId)

                val result = talkMeDownClient.chat(
                    impulse = impulse,
                    desireCurve = desireCurve,
                    returnLog = returnEvents,
                    priorTranscript = priorTranscript,
                    userMessage = text
                )
                _dialogMessages.value = _dialogMessages.value + DialogMessage("assistant", result.response)

                // Apply reclassification silently if the LLM detected a misclassification
                result.reclassification?.let { r ->
                    var updated = impulse
                    r.tier?.let { t ->
                        runCatching { Tier.valueOf(t) }.getOrNull()?.let { updated = updated.copy(tier = it) }
                    }
                    r.category?.let { c ->
                        runCatching { Category.valueOf(c) }.getOrNull()?.let { updated = updated.copy(category = it) }
                    }
                    r.partnerGate?.let { updated = updated.copy(partnerGate = it) }
                    r.partnerReason?.let { updated = updated.copy(partnerReason = it.ifEmpty { null }) }
                    if (updated != impulse) {
                        repository.updateClassification(updated)
                        Log.d("ImpulseDetailVM", "Reclassified via dialog: tier=${updated.tier}, cat=${updated.category}, partner=${updated.partnerGate}")
                    }
                }

                val transcript = Json.encodeToString(
                    JsonArray.serializer(),
                    buildJsonArray {
                        for (msg in _dialogMessages.value) {
                            add(buildJsonObject {
                                put("role", JsonPrimitive(msg.role))
                                put("content", JsonPrimitive(msg.content))
                            })
                        }
                    }
                )
                repository.saveDialogSession(impulseId, transcript)
            } catch (e: Exception) {
                _error.value = "Dialog error: ${e.message}"
            } finally {
                _isDialogLoading.value = false
            }
        }
    }
}
