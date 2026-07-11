package com.egabel.daddont.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.egabel.daddont.DadDontApp
import com.egabel.daddont.data.repository.ImpulseRepository
import com.egabel.daddont.data.repository.Scorecard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val scorecard: Scorecard? = null,
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
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            _uiState.value = StatsUiState(
                scorecard = repository.computeScorecard(thirtyDaysAgo),
                isLoading = false
            )
        }
    }
}
