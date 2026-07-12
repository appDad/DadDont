package com.egabel.daddont.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A slip: acted on the impulse before it finished, but the impulse
 * lives on. Recording it and continuing IS the reinforcement — a slip
 * is data, not defeat. Only an explicit "give up" ends the impulse
 * (Verdict.BROKE).
 */
@Entity(
    tableName = "breach_events",
    foreignKeys = [ForeignKey(
        entity = Impulse::class,
        parentColumns = ["id"],
        childColumns = ["impulseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("impulseId")]
)
data class BreachEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val impulseId: UUID,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String
)
