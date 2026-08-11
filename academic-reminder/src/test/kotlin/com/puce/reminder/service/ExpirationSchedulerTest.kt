package com.puce.reminder.service

import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.repository.ReminderRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExpirationSchedulerTest {
    private val reminders = mock<ReminderRepository>()
    private val scheduler = ExpirationScheduler(reminders)

    @Test
    fun `expires pending reminders`() {
        whenever(reminders.expirePending(any(), eq(ReminderStatus.PENDING), eq(ReminderStatus.EXPIRED))).thenReturn(2)

        scheduler.expirePendingReminders()

        verify(reminders).expirePending(any(), eq(ReminderStatus.PENDING), eq(ReminderStatus.EXPIRED))
    }

    @Test
    fun `handles run without expired reminders`() {
        whenever(reminders.expirePending(any(), eq(ReminderStatus.PENDING), eq(ReminderStatus.EXPIRED))).thenReturn(0)

        scheduler.expirePendingReminders()

        verify(reminders).expirePending(any(), eq(ReminderStatus.PENDING), eq(ReminderStatus.EXPIRED))
    }
}
