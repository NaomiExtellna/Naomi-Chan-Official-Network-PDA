package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_accounts",
    indices = [Index(value = ["username"], unique = true)]
)
data class StaffAccountEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val credentialHash: String,
    val salt: String,
    val role: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val recoveryHash: String? = null,
    val recoverySalt: String? = null,
    @ColumnInfo(defaultValue = "0") val failedAttempts: Int = 0,
    val lockedUntil: Long? = null,
    @ColumnInfo(defaultValue = "0") val mustChangeCredential: Boolean = false,
    @ColumnInfo(defaultValue = "0") val canVoid: Boolean = false,
    @ColumnInfo(defaultValue = "0") val canExport: Boolean = false,
    @ColumnInfo(defaultValue = "0") val canEditVenues: Boolean = false,
    @ColumnInfo(defaultValue = "0") val canChangeGateway: Boolean = false,
    @ColumnInfo(defaultValue = "0") val canViewTotals: Boolean = false
)

@Entity(tableName = "staff_shifts")
data class StaffShiftEntity(
    @PrimaryKey val id: String,
    val staffId: String,
    val staffDisplayName: String,
    val openedAt: Long,
    val closedAt: Long? = null,
    val openingNote: String = "",
    val closingNote: String = ""
)
