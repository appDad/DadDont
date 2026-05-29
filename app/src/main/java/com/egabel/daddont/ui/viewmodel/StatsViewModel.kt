package com.egabel.daddont.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.repository.ImpulseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val killedThisMonth: Int = 0,
    val stillCycling: Int = 0,
    val topOffenders: List<Impulse> = emptyList(),
    val isLoading: Boolean = true
)

class StatsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImpulseRepository((application as DadDontApp).database)

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

            _uiState.value = StatsUiState(
                killedThisMonth = repository.countDismissedSince(thirtyDaysAgo),
                stillCycling = repository.countActive(),
                topOffenders = repository.topRecurrenceOffenders(),
                isLoading = false
            )
        }
    }
}
