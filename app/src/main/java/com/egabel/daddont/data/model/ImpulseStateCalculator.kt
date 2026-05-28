package com.egabel.daddont.data.model

object ImpulseStateCalculator {
    fun computeState(impulse: Impulse, now: Long = System.currentTimeMillis()): ImpulseState {
        if (impulse.dismissedAt != null) return ImpulseState.GRAY

        val tier = impulse.tier ?: return ImpulseState.PENDING
        val start = impulse.classifiedAt ?: impulse.createdAt
        val durations = CoolingConfig.durationsFor(tier)
        val elapsed = now - start

        return when {
            elapsed < durations.redMs -> ImpulseState.RED
            elapsed < durations.redMs + durations.yellowMs -> ImpulseState.YELLOW
            elapsed < durations.redMs + durations.yellowMs + durations.greenMs -> ImpulseState.GREEN
            elapsed >= durations.grayAfterMs -> ImpulseState.GRAY
            else -> ImpulseState.GREEN
        }
    }

    /**
     * Returns milliseconds until the next state transition, or null if
     * the impulse is PENDING, GRAY, or dismissed.
     */
    fun msUntilNextState(impulse: Impulse, now: Long = System.currentTimeMillis()): Long? {
        if (impulse.dismissedAt != null) return null
        val tier = impulse.tier ?: return null
        val start = impulse.classifiedAt ?: impulse.createdAt
        val durations = CoolingConfig.durationsFor(tier)
        val elapsed = now - start

        val nextBoundary = when {
            elapsed < durations.redMs -> durations.redMs
            elapsed < durations.redMs + durations.yellowMs -> durations.redMs + durations.yellowMs
            elapsed < durations.redMs + durations.yellowMs + durations.greenMs -> durations.grayAfterMs
            else -> return null // already gray
        }
        return (nextBoundary - elapsed).coerceAtLeast(0)
    }
}
