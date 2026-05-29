package com.egabel.daddont.data.model

object ImpulseStateCalculator {
    fun computeState(impulse: Impulse, now: Long = System.currentTimeMillis()): ImpulseState {
        if (impulse.dismissedAt != null) return ImpulseState.GRAY

        val tier = impulse.tier ?: return ImpulseState.PENDING
        val start = impulse.classifiedAt ?: impulse.createdAt
        val elapsed = now - start

        // Custom endpoint: split the window proportionally (40% red, 30% yellow, 30% green)
        val customUntil = impulse.customCoolUntil
        if (customUntil != null) {
            val totalMs = (customUntil - start).coerceAtLeast(1)
            val redEnd = (totalMs * 0.40).toLong()
            val yellowEnd = (totalMs * 0.70).toLong()
            return when {
                elapsed < redEnd -> ImpulseState.RED
                elapsed < yellowEnd -> ImpulseState.YELLOW
                elapsed < totalMs -> ImpulseState.GREEN
                else -> ImpulseState.GRAY
            }
        }

        // Standard tier-based durations
        val durations = CoolingConfig.durationsFor(tier)
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
        val elapsed = now - start

        // Custom endpoint
        val customUntil = impulse.customCoolUntil
        if (customUntil != null) {
            val totalMs = (customUntil - start).coerceAtLeast(1)
            val redEnd = (totalMs * 0.40).toLong()
            val yellowEnd = (totalMs * 0.70).toLong()
            val nextBoundary = when {
                elapsed < redEnd -> redEnd
                elapsed < yellowEnd -> yellowEnd
                elapsed < totalMs -> totalMs
                else -> return null
            }
            return (nextBoundary - elapsed).coerceAtLeast(0)
        }

        // Standard tier-based
        val durations = CoolingConfig.durationsFor(tier)
        val nextBoundary = when {
            elapsed < durations.redMs -> durations.redMs
            elapsed < durations.redMs + durations.yellowMs -> durations.redMs + durations.yellowMs
            elapsed < durations.redMs + durations.yellowMs + durations.greenMs -> durations.grayAfterMs
            else -> return null
        }
        return (nextBoundary - elapsed).coerceAtLeast(0)
    }
}
