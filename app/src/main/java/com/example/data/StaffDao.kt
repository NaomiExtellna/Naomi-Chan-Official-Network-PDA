package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT COUNT(*) FROM staff_accounts")
    suspend fun countStaff(): Int

    @Query("SELECT * FROM staff_accounts WHERE lower(username) = lower(:username) LIMIT 1")
    suspend fun findByUsername(username: String): StaffAccountEntity?

    @Query("SELECT * FROM staff_accounts WHERE id = :staffId LIMIT 1")
    suspend fun findById(staffId: String): StaffAccountEntity?

    @Query("SELECT * FROM staff_accounts ORDER BY CASE role WHEN 'ADMIN' THEN 0 ELSE 1 END, displayName COLLATE NOCASE")
    fun observeAllStaff(): Flow<List<StaffAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStaff(account: StaffAccountEntity)

    @Query("UPDATE staff_accounts SET lastLoginAt = :timestamp, failedAttempts = 0, lockedUntil = NULL WHERE id = :staffId")
    suspend fun markLoginSuccess(staffId: String, timestamp: Long)

    @Query("UPDATE staff_accounts SET failedAttempts = :attempts, lockedUntil = :lockedUntil WHERE id = :staffId")
    suspend fun updateLoginFailure(staffId: String, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE staff_accounts SET failedAttempts = 0, lockedUntil = NULL WHERE id = :staffId")
    suspend fun clearLoginLock(staffId: String)

    @Query("UPDATE staff_accounts SET isActive = :isActive WHERE id = :staffId")
    suspend fun setStaffActive(staffId: String, isActive: Boolean)

    @Query("UPDATE staff_accounts SET credentialHash = :hash, salt = :salt, mustChangeCredential = :mustChange, failedAttempts = 0, lockedUntil = NULL WHERE id = :staffId")
    suspend fun updateCredential(staffId: String, hash: String, salt: String, mustChange: Boolean)

    @Query("UPDATE staff_accounts SET recoveryHash = :hash, recoverySalt = :salt WHERE id = :staffId")
    suspend fun updateRecovery(staffId: String, hash: String, salt: String)

    @Query("UPDATE staff_accounts SET canVoid = :canVoid, canExport = :canExport, canEditVenues = :canEditVenues, canChangeGateway = :canChangeGateway, canViewTotals = :canViewTotals WHERE id = :staffId")
    suspend fun updatePermissions(
        staffId: String,
        canVoid: Boolean,
        canExport: Boolean,
        canEditVenues: Boolean,
        canChangeGateway: Boolean,
        canViewTotals: Boolean
    )

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: StaffShiftEntity)

    @Query("SELECT * FROM staff_shifts WHERE staffId = :staffId AND closedAt IS NULL ORDER BY openedAt DESC LIMIT 1")
    suspend fun getOpenShiftForStaff(staffId: String): StaffShiftEntity?

    @Query("SELECT * FROM staff_shifts ORDER BY openedAt DESC")
    fun observeAllShifts(): Flow<List<StaffShiftEntity>>

    @Query("UPDATE staff_shifts SET closedAt = :closedAt, closingNote = :closingNote WHERE id = :shiftId AND closedAt IS NULL")
    suspend fun closeShift(shiftId: String, closedAt: Long, closingNote: String)
}
