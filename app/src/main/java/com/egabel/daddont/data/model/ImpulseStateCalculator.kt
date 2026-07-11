package com.egabel.daddont.data.model

/**
 * Verdict-required lifecycle:
 *
 *   PENDING — awaiting classification (no decideBy yet)
 *   RED     — first half of the cooling window
 *   YELLOW  — second half of the cooling window
 *   GREEN   — past decideBy. A verdict is DUE. This state never expires;
 *             it escalates (overdue time grows) until the user decides.
 *   GRAY    — verdict recorded. The ONLY way in is an explicit decision.
 */
object ImpulseStateCalculator {

    fun computeState(impulse: Impulse, now: Long = System.currentTimeMillis()): ImpulseState {
        if (impulse.verdict != null) return ImpulseState.GRAY

        val decideBy = impulse.decideBy ?: return ImpulseState.PENDING
        val start = impulse.classifiedAt ?: impulse.createdAt

        if (now >= decideBy) return ImpulseState.GREEN

        val window = (decideBy - start).coerceAtLeast(1)
        val elapsed = now - start
        return if (elapsed < window / 2) ImpulseState.RED else ImpulseState.YELLOW
    }

    /**
     * Milliseconds until the next state transition (RED→YELLOW or →GREEN),
     * or null when PENDING, GREEN (nothing "next" — a decision is due), or GRAY.
     */
    fun msUntilNextState(impulse: Impulse, now: Long = System.currentTimeMillis()): Long? {
        if (impulse.verdict != null) return null
        val decideBy = impulse.decideBy ?: return null
        if (now >= decideBy) return null

        val start = impulse.classifiedAt ?: impulse.createdAt
        val window = (decideBy - start).coerceAtLeast(1)
        val elapsed = now - start
        val halfway = window / 2

        val nextBoundary = if (elapsed < halfway) start + halfway else decideBy
        return (nextBoundary - now).coerceAtLeast(0)
    }

    /** How long a GREEN impulse has been awaiting a verdict; null otherwise. */
    fun overdueMs(impulse: Impulse, now: Long = System.currentTimeMillis()): Long? {
        if (impulse.verdict != null) return null
        val decideBy = impulse.decideBy ?: return null
        return if (now >= decideBy) now - decideBy else null
    }
}
