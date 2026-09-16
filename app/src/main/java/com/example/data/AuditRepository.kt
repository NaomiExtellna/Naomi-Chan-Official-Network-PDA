package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AuditRepository(private val auditDao: AuditDao) {
    val recentEvents: Flow<List<AuditEvent>> = auditDao.observeRecent().map { events ->
        events.map { it.toModel() }
    }

    suspend fun log(
        actorId: String? = null,
        actorName: String = "System",
        action: String,
        target: String = "",
        details: String = "",
        severity: String = "INFO"
    ) = withContext(Dispatchers.IO) {
        auditDao.insert(
            AuditEventEntity(
                id = UUID.randomUUID().toString(),
                actorId = actorId,
                actorName = actorName.ifBlank { "System" },
                action = action,
                target = target,
                details = details.take(1000),
                severity = severity.uppercase()
            )
        )
    }

    suspend fun pruneOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - days.coerceAtLeast(1) * 86_400_000L
        auditDao.pruneBefore(cutoff)
    }
}
