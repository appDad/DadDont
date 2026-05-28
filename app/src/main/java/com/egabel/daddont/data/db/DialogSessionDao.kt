package com.egabel.daddont.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.egabel.daddont.data.model.DialogSession
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface DialogSessionDao {
    @Insert
    suspend fun insert(session: DialogSession)

    @Query("SELECT * FROM dialog_sessions WHERE impulseId = :impulseId ORDER BY timestamp DESC")
    fun observeForImpulse(impulseId: UUID): Flow<List<DialogSession>>

    @Query("SELECT * FROM dialog_sessions WHERE impulseId = :impulseId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForImpulse(impulseId: UUID): DialogSession?
}
