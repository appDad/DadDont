package com.egabel.daddont.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.egabel.daddont.data.model.DesireCheckIn
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface DesireCheckInDao {
    @Insert
    suspend fun insert(checkIn: DesireCheckIn)

    @Query("SELECT * FROM desire_check_ins WHERE impulseId = :impulseId ORDER BY timestamp ASC")
    fun observeForImpulse(impulseId: UUID): Flow<List<DesireCheckIn>>

    @Query("SELECT * FROM desire_check_ins WHERE impulseId = :impulseId ORDER BY timestamp ASC")
    suspend fun getForImpulse(impulseId: UUID): List<DesireCheckIn>

    @Query("SELECT * FROM desire_check_ins")
    suspend fun getAll(): List<DesireCheckIn>
}
