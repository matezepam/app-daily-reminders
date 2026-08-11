package com.puce.reminder.audit

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AuditServiceTest {
    @Test
    fun `stores academic audit entry`() {
        val repository = mock<AuditLogRepository>()
        AuditService(repository).record("professor-1", "DELETE", "Course", 9, "old", null)
        verify(repository).save(argThat { userSub == "professor-1" && action == "DELETE" && entityType == "Course" && entityId == "9" })
    }
}
