package com.puce.reminder.audit

import org.springframework.stereotype.Service

@Service
class AuditService(private val repository: AuditLogRepository) {
    fun record(userSub: String, action: String, entityType: String, entityId: Any, previous: String? = null, current: String? = null) {
        repository.save(AuditLog(userSub, action, entityType, entityId.toString(), previous, current))
    }
}
