package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_events",
    indices = [Index(value = ["createdAt"]), Index(value = ["actorId"])]
)
data class AuditEventEntity(
    @PrimaryKey val id: String,
    val createdAt: Long = System.currentTimeMillis(),
    val actorId: String? = null,
    val actorName: String = "System",
    val action: String,
    val target: String = "",
    val details: String = "",
    val severity: String = "INFO"
)

data class AuditEvent(
    val id: String,
    val createdAt: Long,
    val actorId: String?,
    val actorName: String,
    val action: String,
    val target: String,
    val details: String,
    val severity: String
)

fun AuditEventEntity.toModel(): AuditEvent = AuditEvent(
    id = id,
    createdAt = createdAt,
    actorId = actorId,
    actorName = actorName,
    action = action,
    target = target,
    details = details,
    severity = severity
)
