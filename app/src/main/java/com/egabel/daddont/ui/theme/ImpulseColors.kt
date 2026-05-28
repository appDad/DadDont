package com.egabel.daddont.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.egabel.daddont.data.model.ImpulseState

object ImpulseColors {
    fun borderColor(state: ImpulseState): Color = when (state) {
        ImpulseState.PENDING -> PendingState
        ImpulseState.RED -> RedState
        ImpulseState.YELLOW -> YellowState
        ImpulseState.GREEN -> GreenState
        ImpulseState.GRAY -> GrayState
    }

    fun containerColor(state: ImpulseState): Color = when (state) {
        ImpulseState.PENDING -> PendingStateContainer
        ImpulseState.RED -> RedStateContainer
        ImpulseState.YELLOW -> YellowStateContainer
        ImpulseState.GREEN -> GreenStateContainer
        ImpulseState.GRAY -> GrayStateContainer
    }

    fun label(state: ImpulseState): String = when (state) {
        ImpulseState.PENDING -> "Classifying…"
        ImpulseState.RED -> "Hot"
        ImpulseState.YELLOW -> "Cooling"
        ImpulseState.GREEN -> "Cooled"
        ImpulseState.GRAY -> "Archived"
    }
}
