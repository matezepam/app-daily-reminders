package com.puce.users.audit

import org.springframework.stereotype.Service

@Service
class AuditService(private val repository: AuditLogRepository) {
    fun record(userSub: String, action: String, entityId: String, previousValues: String?, newValues: String?) {
        repository.save(AuditLog(userSub, action, "UserProfile", entityId, previousValues, newValues))
    }
}
