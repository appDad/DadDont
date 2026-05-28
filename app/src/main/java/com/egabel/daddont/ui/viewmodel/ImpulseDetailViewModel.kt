package com.egabel.daddont.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.api.gemini.TalkMeDownClient
import com.egabel.daddont.api.tasks.GoogleTasksClient
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.ReturnEvent
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.data.repository.ImpulseWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class DialogMessage(val role: String, val content: String)

data class ImpulseDetailUiState(
    val impulseWithState: ImpulseWithState? = null,
    val returnEvents: List<ReturnEvent> = emptyList(),
    val dialogMessages: List<DialogMessage> = emptyList(),
    val isDialogLoading: Boolean = false,
    val dialogInput: String = "",
    val showTalkMeDown: Boolean = false,
    val sentToDadDo: Boolean = false,
    val error: String? = null
)

class ImpulseDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = ImpulseRepository((application as DadDontApp).database)
    private val talkMeDownClient = TalkMeDownClient()
    val tasksClient = GoogleTasksClient(application)

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

    val uiState: StateFlow<ImpulseDetailUiState> = combine(
        impulseFlow, returnEventsFlow, _dialogMessages, _isDialogLoading,
        _dialogInput, _showTalkMeDown, _error
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        ImpulseDetailUiState(
            impulseWithState = values[0] as ImpulseWithState?,
            returnEvents = values[1] as List<ReturnEvent>,
            dialogMessages = values[2] as List<DialogMessage>,
            isDialogLoading = values[3] as Boolean,
            dialogInput = values[4] as String,
            showTalkMeDown = values[5] as Boolean,
            sentToDadDo = (values[0] as ImpulseWithState?)?.impulse?.sentToDadDoAt != null,
            error = values[6] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImpulseDetailUiState())

    fun setDialogInput(text: String) { _dialogInput.value = text }
    fun toggleTalkMeDown() { _showTalkMeDown.value = !_showTalkMeDown.value }
    fun clearError() { _error.value = null }

    fun recordReturn(rationale: String? = null) {
        viewModelScope.launch {
            repository.recordReturn(impulseId, rationale)
        }
    }

    fun dismiss(type: DismissalType) {
        viewModelScope.launch {
            repository.dismiss(impulseId, type)
        }
    }

    fun toggleRamonaFlag() {
        viewModelScope.launch {
            repository.toggleRamonaFlag(impulseId)
        }
    }

    fun sendToDadDo(activity: Activity) {
        viewModelScope.launch {
            try {
                val impulse = repository.getById(impulseId)?.impulse ?: return@launch
                tasksClient.sendToDadDo(impulse.content, activity)
                repository.markSentToDadDo(impulseId)
            } catch (e: Exception) {
                _error.value = "Failed to send to DadDo: ${e.message}"
            }
        }
    }

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
                val priorTranscript = repository.getLatestDialogTranscript(impulseId)

                val response = talkMeDownClient.chat(
                    impulseText = impulse.content,
                    returnLog = returnEvents,
                    priorTranscript = priorTranscript,
                    userMessage = text
                )
                _dialogMessages.value = _dialogMessages.value + DialogMessage("assistant", response)

                // Persist transcript
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
