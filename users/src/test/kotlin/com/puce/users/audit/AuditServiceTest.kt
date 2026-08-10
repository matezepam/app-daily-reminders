package com.puce.users.audit

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class AuditServiceTest {
    @Test
    fun `stores sanitized audit values`() {
        val repository = mock<AuditLogRepository>()
        AuditService(repository).record("sub-1", "UPDATE", "1", "old", "new")
        verify(repository).save(argThat { userSub == "sub-1" && action == "UPDATE" && previousValues == "old" && newValues == "new" })
    }
}
