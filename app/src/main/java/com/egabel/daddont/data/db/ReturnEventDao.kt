package com.egabel.daddont.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.egabel.daddont.data.model.ReturnEvent
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ReturnEventDao {
    @Insert
    suspend fun insert(event: ReturnEvent)

    @Query("SELECT * FROM return_events WHERE impulseId = :impulseId ORDER BY timestamp DESC")
    fun observeForImpulse(impulseId: UUID): Flow<List<ReturnEvent>>

    @Query("SELECT * FROM return_events WHERE impulseId = :impulseId ORDER BY timestamp DESC")
    suspend fun getForImpulse(impulseId: UUID): List<ReturnEvent>
}
