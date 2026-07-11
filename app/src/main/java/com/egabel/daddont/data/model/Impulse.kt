package com.egabel.daddont.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "impulses")
data class Impulse(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val createdAt: Long = System.currentTimeMillis(),
    val content: String,

    // Classification
    val tier: Tier? = null,
    val category: Category? = null,
    val classifiedAt: Long? = null,
    val ungraded: Boolean = true,

    // Cooling contract — the moment a verdict becomes due.
    // Set at classification (tier window), by defer, or by a custom pick.
    val decideBy: Long? = null,

    // Mental-state snapshot at capture
    val desireAtCapture: Int? = null,      // 1-10
    val trigger: String? = null,           // what likely prompted this
    val rationale: String? = null,         // their stated reason for wanting it
    val estimatedCost: Double? = null,     // USD, for purchases
    val prediction: Prediction? = null,    // what they think they'll feel when cooled

    // Partner gate
    val partnerGate: Boolean = false,
    val partnerReason: String? = null,

    // History
    val returnCount: Int = 0,
    val reactivationCount: Int = 0,
    val deferCount: Int = 0,

    // Verdict — the only way an impulse leaves the active list
    val verdict: Verdict? = null,
    val verdictAt: Long? = null,
    val verdictNote: String? = null
)
