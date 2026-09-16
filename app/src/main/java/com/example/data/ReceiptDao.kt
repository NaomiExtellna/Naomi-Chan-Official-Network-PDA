package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {
    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    fun getAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE syncStatus != 'SYNCED' AND receiptStatus != 'VOID' ORDER BY createdAt ASC")
    suspend fun getUnsyncedReceipts(): List<ReceiptEntity>

    @Query("SELECT COUNT(*) FROM receipts WHERE syncStatus != 'SYNCED' AND receiptStatus != 'VOID'")
    fun getUnsyncedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReceiptIgnore(receipt: ReceiptEntity): Long

    @Update
    suspend fun updateReceipt(receipt: ReceiptEntity)

    @Query("UPDATE receipts SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE receipts SET isPrinted = :isPrinted, printedChannel = :channel WHERE id = :id")
    suspend fun updatePrintStatus(id: String, isPrinted: Boolean, channel: String)

    @Query("UPDATE receipts SET receiptStatus = 'VOID', voidReason = :reason, voidedAt = :voidedAt, voidedBy = :voidedBy WHERE id = :id AND receiptStatus != 'VOID'")
    suspend fun voidReceipt(id: String, reason: String, voidedAt: Long, voidedBy: String): Int

    @Query("UPDATE receipts SET receiptStatus = 'ARCHIVED' WHERE syncStatus = 'SYNCED' AND receiptStatus = 'ACTIVE' AND createdAt < :cutoff")
    suspend fun archiveSyncedBefore(cutoff: Long): Int

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteReceipt(id: String)

    @Query("DELETE FROM receipts")
    suspend fun deleteAllReceipts(): Int
}
