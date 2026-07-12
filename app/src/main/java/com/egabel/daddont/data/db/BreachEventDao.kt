package com.egabel.daddont.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.egabel.daddont.data.model.BreachEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface BreachEventDao {
    @Insert
    suspend fun insert(event: BreachEvent)

    @Query("SELECT * FROM breach_events WHERE impulseId = :impulseId ORDER BY timestamp ASC")
    fun observeForImpulse(impulseId: UUID): Flow<List<BreachEvent>>

    @Query("SELECT * FROM breach_events WHERE impulseId = :impulseId ORDER BY timestamp ASC")
    suspend fun getForImpulse(impulseId: UUID): List<BreachEvent>

    @Query("SELECT * FROM breach_events ORDER BY timestamp DESC")
    suspend fun getAll(): List<BreachEvent>

    @Query("SELECT COUNT(*) FROM breach_events WHERE timestamp >= :since")
    suspend fun countSince(since: Long): Int

    @Query("SELECT MAX(timestamp) FROM breach_events")
    suspend fun lastSlipAt(): Long?
}
