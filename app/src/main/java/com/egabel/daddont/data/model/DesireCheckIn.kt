package com.egabel.daddont.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A point on the desire curve. The first one is written at capture time;
 * later ones come from returns and check-ins. The decay between the first
 * and latest reading is the core evidence for killing an impulse.
 */
@Entity(
    tableName = "desire_check_ins",
    foreignKeys = [ForeignKey(
        entity = Impulse::class,
        parentColumns = ["id"],
        childColumns = ["impulseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("impulseId")]
)
data class DesireCheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val impulseId: UUID,
    val timestamp: Long = System.currentTimeMillis(),
    val strength: Int // 1-10
)
