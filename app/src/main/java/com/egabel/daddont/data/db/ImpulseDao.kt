package com.egabel.daddont.data.db

import androidx.room.Dao
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

    @Query("SELECT * FROM impulses WHERE dismissedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE dismissedAt IS NOT NULL ORDER BY dismissedAt DESC")
    fun observeArchived(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE partnerGate = 1 ORDER BY createdAt DESC")
    fun observePartnerFlagged(): Flow<List<Impulse>>

    @Query("SELECT * FROM impulses WHERE ungraded = 1")
    suspend fun getUngraded(): List<Impulse>

    @Query("SELECT * FROM impulses WHERE id = :id")
    suspend fun getById(id: UUID): Impulse?

    @Query("SELECT * FROM impulses WHERE id = :id")
    fun observeById(id: UUID): Flow<Impulse?>

    @Query("SELECT * FROM impulses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Impulse>>

    @Query("SELECT COUNT(*) FROM impulses WHERE dismissedAt IS NOT NULL AND dismissedAt > :since")
    suspend fun countDismissedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM impulses WHERE sentToDadDoAt IS NOT NULL AND sentToDadDoAt > :since")
    suspend fun countExecutedSince(since: Long): Int

    @Query("SELECT COUNT(*) FROM impulses WHERE dismissedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT * FROM impulses WHERE dismissedAt IS NULL ORDER BY returnCount DESC LIMIT :limit")
    suspend fun topRecurrenceOffenders(limit: Int = 10): List<Impulse>
}
