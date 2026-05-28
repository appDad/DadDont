package com.egabel.daddont.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.data.model.DismissalType
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.data.repository.ImpulseWithState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ListFilter { ACTIVE, ARCHIVE, RAMONA }

data class ImpulseListUiState(
    val impulses: List<ImpulseWithState> = emptyList(),
    val filter: ListFilter = ListFilter.ACTIVE,
    val captureText: String = "",
    val isCapturing: Boolean = false
)

class ImpulseListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImpulseRepository((application as DadDontApp).database)

    private val _filter = MutableStateFlow(ListFilter.ACTIVE)
    private val _captureText = MutableStateFlow("")
    private val _isCapturing = MutableStateFlow(false)

    private val activeImpulses = repository.observeActiveWithState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val archivedImpulses = repository.observeArchivedWithState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val ramonaImpulses = repository.observeRamonaFlagged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ImpulseListUiState> = combine(
        _filter, _captureText, _isCapturing, activeImpulses, archivedImpulses, ramonaImpulses
    ) { values ->
        val filter = values[0] as ListFilter
        val captureText = values[1] as String
        val isCapturing = values[2] as Boolean
        @Suppress("UNCHECKED_CAST")
        val impulses = when (filter) {
            ListFilter.ACTIVE -> values[3] as List<ImpulseWithState>
            ListFilter.ARCHIVE -> values[4] as List<ImpulseWithState>
            ListFilter.RAMONA -> values[5] as List<ImpulseWithState>
        }
        ImpulseListUiState(
            impulses = impulses,
            filter = filter,
            captureText = captureText,
            isCapturing = isCapturing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImpulseListUiState())

    fun setFilter(filter: ListFilter) {
        _filter.value = filter
    }

    fun setCaptureText(text: String) {
        _captureText.value = text
    }

    fun setCapturing(capturing: Boolean) {
        _isCapturing.value = capturing
    }

    fun captureImpulse() {
        val text = _captureText.value.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            repository.capture(text)
            _captureText.value = ""
        }
    }

    fun captureVoiceResult(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.capture(text.trim())
        }
    }

    fun recordReturn(impulseId: UUID, rationale: String? = null) {
        viewModelScope.launch {
            repository.recordReturn(impulseId, rationale)
        }
    }

    fun dismiss(impulseId: UUID, type: DismissalType) {
        viewModelScope.launch {
            repository.dismiss(impulseId, type)
        }
    }

    fun toggleRamonaFlag(impulseId: UUID) {
        viewModelScope.launch {
            repository.toggleRamonaFlag(impulseId)
        }
    }
}
