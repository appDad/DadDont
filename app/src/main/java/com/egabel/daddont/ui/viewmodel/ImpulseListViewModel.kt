package com.egabel.daddont.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.api.gemini.GeminiClient
import com.egabel.daddont.data.model.Category
import com.egabel.daddont.data.model.ImpulseState
import com.egabel.daddont.data.model.Prediction
import com.egabel.daddont.data.model.Tier
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
import java.util.UUID

enum class ListFilter { ACTIVE, ARCHIVE, PARTNER }

data class ImpulseListUiState(
    val decide: List<ImpulseWithState> = emptyList(),   // GREEN — verdicts due, pinned on top
    val cooling: List<ImpulseWithState> = emptyList(),  // PENDING/RED/YELLOW
    val impulses: List<ImpulseWithState> = emptyList(), // archive / partner tabs
    val filter: ListFilter = ListFilter.ACTIVE,
    val captureText: String = "",
    // Quick-facts sheet shown right after a capture
    val pendingFactsFor: UUID? = null,
    val pendingFactsContent: String = ""
)

class ImpulseListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImpulseRepository((application as DadDontApp).database)
    private val gemini = GeminiClient(application)

    private val _filter = MutableStateFlow(ListFilter.ACTIVE)
    private val _captureText = MutableStateFlow("")
    private val _pendingFacts = MutableStateFlow<Pair<UUID, String>?>(null)

    private val activeImpulses = repository.observeActiveWithState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val archivedImpulses = repository.observeArchivedWithState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val partnerImpulses = repository.observePartnerFlagged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<ImpulseListUiState> = combine(
        _filter, _captureText, _pendingFacts, activeImpulses, archivedImpulses, partnerImpulses
    ) { values ->
        val filter = values[0] as ListFilter
        val captureText = values[1] as String
        @Suppress("UNCHECKED_CAST")
        val pendingFacts = values[2] as Pair<UUID, String>?
        @Suppress("UNCHECKED_CAST")
        val active = values[3] as List<ImpulseWithState>
        @Suppress("UNCHECKED_CAST")
        val archived = values[4] as List<ImpulseWithState>
        @Suppress("UNCHECKED_CAST")
        val partner = values[5] as List<ImpulseWithState>

        when (filter) {
            ListFilter.ACTIVE -> {
                val (decide, cooling) = active.partition { it.state == ImpulseState.GREEN }
                ImpulseListUiState(
                    decide = decide.sortedByDescending { it.overdueMs ?: 0 },
                    cooling = cooling,
                    filter = filter,
                    captureText = captureText,
                    pendingFactsFor = pendingFacts?.first,
                    pendingFactsContent = pendingFacts?.second ?: ""
                )
            }
            ListFilter.ARCHIVE -> ImpulseListUiState(
                impulses = archived, filter = filter, captureText = captureText,
                pendingFactsFor = pendingFacts?.first,
                pendingFactsContent = pendingFacts?.second ?: ""
            )
            ListFilter.PARTNER -> ImpulseListUiState(
                impulses = partner, filter = filter, captureText = captureText,
                pendingFactsFor = pendingFacts?.first,
                pendingFactsContent = pendingFacts?.second ?: ""
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ImpulseListUiState())

    fun setFilter(filter: ListFilter) {
        _filter.value = filter
    }

    fun setCaptureText(text: String) {
        _captureText.value = text
    }

    fun captureImpulse() {
        val text = _captureText.value.trim()
        if (text.isEmpty()) return
        _captureText.value = ""
        capture(text)
    }

    fun captureVoiceResult(text: String) {
        if (text.isBlank()) return
        capture(text.trim())
    }

    private fun capture(text: String) {
        viewModelScope.launch {
            val impulse = repository.capture(text)
            // Show the quick-facts sheet immediately; classify in parallel
            _pendingFacts.value = impulse.id to text
            WidgetUpdater.updateAll(getApplication())
            classifyNow(impulse.id, text)
        }
    }

    /** Quick-facts sheet result: desire slider, cost, prediction. */
    fun saveFacts(impulseId: UUID, desire: Int, cost: Double?, prediction: Prediction?) {
        _pendingFacts.value = null
        viewModelScope.launch {
            repository.updateFacets(impulseId, desire, cost, prediction)
        }
    }

    fun skipFacts() {
        _pendingFacts.value = null
    }

    private suspend fun classifyNow(impulseId: UUID, content: String) {
        try {
            val c = gemini.classify(content) ?: return
            val impulse = repository.getById(impulseId)?.impulse ?: return
            val classified = repository.applyClassification(
                impulse.copy(
                    tier = Tier.valueOf(c.tier),
                    category = Category.valueOf(c.category),
                    partnerGate = c.partnerGate,
                    partnerReason = c.partnerReason.ifEmpty { null },
                    trigger = c.trigger ?: impulse.trigger,
                    rationale = c.rationale ?: impulse.rationale,
                    estimatedCost = impulse.estimatedCost ?: c.estimatedCostUsd,
                    desireAtCapture = impulse.desireAtCapture ?: c.desireStrength
                )
            )
            classified.decideBy?.let {
                VerdictWorker.schedule(getApplication(), classified.id, it)
            }
            WidgetUpdater.updateAll(getApplication())
        } catch (e: Exception) {
            Log.e("ImpulseListVM", "Immediate classification failed, WorkManager will retry", e)
        }
    }
}
