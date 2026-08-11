package com.puce.reminder.repository

import jakarta.persistence.LockModeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.Lock

class ReminderRepositoryLockTest {
    @Test
    fun `completion mutations lock the reminder row`() {
        val method = ReminderRepository::class.java.getMethod(
            "findByIdForUpdate",
            Long::class.javaPrimitiveType,
        )

        assertEquals(LockModeType.PESSIMISTIC_WRITE, method.getAnnotation(Lock::class.java).value)
    }
}
