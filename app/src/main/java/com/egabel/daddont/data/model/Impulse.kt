package com.egabel.daddont.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "impulses")
data class Impulse(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val createdAt: Long = System.currentTimeMillis(),
    val content: String,
    val tier: Tier? = null,
    val category: Category? = null,
    val classifiedAt: Long? = null,
    val partnerGate: Boolean = false,
    val partnerReason: String? = null,
    val ungraded: Boolean = true,
    val returnCount: Int = 0,
    val reactivationCount: Int = 0,
    val sentToDadDoAt: Long? = null,
    val dismissedAt: Long? = null,
    val dismissalType: DismissalType? = null
)
