package com.egabel.daddont.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.egabel.daddont.data.model.Impulse
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ImpulseDao {
    @Insert
    suspend fun insert(impulse: Impulse)

    @Update
    suspend fun update(impulse: Impulse)

    @Delete
    suspend fun delete(impulse: Impulse)

    @Query("SELECT * FROM impulses WHERE verdict IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE verdict IS NOT NULL ORDER BY verdictAt DESC")
    fun observeArchived(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE partnerGate = 1 AND verdict IS NULL ORDER BY createdAt DESC")
    fun observePartnerFlagged(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE ungraded = 1")
    suspend fun getUngraded(): List<Impulse>

    @Query("SELECT * FROM impulses WHERE id = :id")
    suspend fun getById(id: UUID): Impulse?

    @Query("SELECT * FROM impulses WHERE id = :id")
    fun observeById(id: UUID): Flow<Impulse?>

    @Query("SELECT * FROM impulses")
    suspend fun getAll(): List<Impulse>

    @Query("SELECT * FROM impulses WHERE verdict IS NULL AND decideBy IS NOT NULL AND decideBy <= :now")
    suspend fun getAwaitingVerdict(now: Long): List<Impulse>

    @Query("SELECT * FROM impulses WHERE verdict IS NULL ORDER BY returnCount DESC LIMIT :limit")
    suspend fun topRecurrenceOffenders(limit: Int = 10): List<Impulse>
}
