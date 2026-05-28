package com.egabel.daddont.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "return_events",
    foreignKeys = [ForeignKey(
        entity = Impulse::class,
        parentColumns = ["id"],
        childColumns = ["impulseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("impulseId")]
)
data class ReturnEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val impulseId: UUID,
    val timestamp: Long = System.currentTimeMillis(),
    val rationale: String? = null
)
