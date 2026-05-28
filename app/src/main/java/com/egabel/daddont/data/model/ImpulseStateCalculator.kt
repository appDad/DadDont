package com.egabel.daddont.data.model

object ImpulseStateCalculator {
    fun computeState(impulse: Impulse, now: Long = System.currentTimeMillis()): ImpulseState {
        if (impulse.dismissedAt != null) return ImpulseState.GRAY

        val tier = impulse.tier ?: return ImpulseState.YELLOW
        val durations = CoolingConfig.durationsFor(tier)
        val elapsed = now - impulse.createdAt

        return when {
            elapsed < durations.redMs -> ImpulseState.RED
            elapsed < durations.redMs + durations.yellowMs -> ImpulseState.YELLOW
            elapsed < durations.redMs + durations.yellowMs + durations.greenMs -> ImpulseState.GREEN
            elapsed >= durations.grayAfterMs -> ImpulseState.GRAY
            else -> ImpulseState.GREEN
        }
    }
}
