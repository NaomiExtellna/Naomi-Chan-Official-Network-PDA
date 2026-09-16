package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: AuditEventEntity)

    @Query("SELECT * FROM audit_events ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 250): Flow<List<AuditEventEntity>>

    @Query("SELECT * FROM audit_events ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 250): List<AuditEventEntity>

    @Query("DELETE FROM audit_events WHERE createdAt < :cutoff")
    suspend fun pruneBefore(cutoff: Long): Int
}
