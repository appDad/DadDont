package com.egabel.daddont.data.model

data class TierDurations(
    val redMs: Long,
    val yellowMs: Long,
    val greenMs: Long,
    val grayAfterMs: Long
)

object CoolingConfig {
    private const val HOUR = 3_600_000L
    private const val DAY = 86_400_000L

    var low = TierDurations(
        redMs = 1 * HOUR,
        yellowMs = 3 * HOUR,
        greenMs = 12 * HOUR,
        grayAfterMs = 24 * HOUR
    )

    var medium = TierDurations(
        redMs = 1 * DAY,
        yellowMs = 2 * DAY,
        greenMs = 4 * DAY,
        grayAfterMs = 7 * DAY
    )

    var high = TierDurations(
        redMs = 3 * DAY,
        yellowMs = 7 * DAY,
        greenMs = 14 * DAY,
        grayAfterMs = 30 * DAY
    )

    fun durationsFor(tier: Tier): TierDurations = when (tier) {
        Tier.LOW -> low
        Tier.MEDIUM -> medium
        Tier.HIGH -> high
    }
}
