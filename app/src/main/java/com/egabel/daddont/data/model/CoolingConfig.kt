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
}
