package com.egabel.daddont.data.model

/**
 * Tier determines the default decision window: how long an impulse cools
 * before a verdict is due. Within the window, the first half is RED and
 * the second half is YELLOW; past decideBy it's GREEN until a verdict.
 */
object CoolingConfig {
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L

    var lowWindowMs = 24 * HOUR
    var mediumWindowMs = 7 * DAY
    var highWindowMs = 30 * DAY

    fun windowFor(tier: Tier): Long = when (tier) {
        Tier.LOW -> lowWindowMs
        Tier.MEDIUM -> mediumWindowMs
        Tier.HIGH -> highWindowMs
    }

    /**
     * Fallback end time for a HOLD when no explicit time could be parsed:
     * 9pm tonight, or 9pm tomorrow if that's already passed ("after the
     * kids are asleep" energy).
     */
    fun defaultHoldEnd(now: Long = System.currentTimeMillis()): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 21)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now) cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }
}
