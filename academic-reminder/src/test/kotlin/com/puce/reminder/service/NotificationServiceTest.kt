package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.Notification
import com.puce.reminder.entity.ReminderType
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.mapper.NotificationMapper
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.repository.NotificationRepository
import com.puce.reminder.repository.ReminderRepository
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class NotificationServiceTest {
    private val repository = mock<NotificationRepository>()
    private val reminders = mock<ReminderRepository>()
    private val access = mock<AccessService>()
    private val currentUser = mock<CurrentUser>()
    private val audit = mock<AuditService>()
    private val service = NotificationService(repository, reminders, access, currentUser, NotificationMapper(), audit)

    @Test
    fun `rejects scheduling for expired reminder`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Expired", type = ReminderType.TASK, dueAt = Instant.now().minusSeconds(60))
        assertThrows(BadRequestException::class.java) { service.replaceFor(reminder, "student-1", setOf(60)) }
    }

    @Test
    fun `missing notification cannot be deleted`() {
        assertThrows(NotFoundException::class.java) { service.delete(99) }
    }

    @Test
    fun `replaces future notification schedule`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Future", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(10800))
        whenever(repository.saveAll(any<List<Notification>>())).thenAnswer { invocation -> invocation.getArgument<List<Notification>>(0).onEachIndexed { index, n -> n.id = index + 1L } }
        val result = service.replaceFor(reminder, "student-1", setOf(60, 120))
        assertEquals(2, result.size)
        inOrder(repository) {
            verify(repository).deleteAllByReminderIdAndTargetUserId(1, "student-1")
            verify(repository).flush()
            verify(repository).saveAll(any<List<Notification>>())
        }
    }

    @Test
    fun `lists and deletes own notifications`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Future", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(7200))
        val notification = Notification(id = 5, reminder = reminder, targetUserId = "student-1", notifyAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(1)).thenReturn(java.util.Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByReminderIdAndTargetUserIdOrderByNotifyAtAsc(1, "student-1")).thenReturn(listOf(notification))
        assertEquals(5, service.list(1).single().id)
        whenever(repository.findByIdAndTargetUserId(5, "student-1")).thenReturn(notification)
        service.delete(5)
        verify(repository).delete(notification)
    }

    @Test
    fun `lists upcoming notifications and cancels future ones`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByTargetUserIdAndCancelledAtIsNullAndNotifyAtAfterOrderByNotifyAtAsc(eq("student-1"), any())).thenReturn(emptyList())
        assertEquals(0, service.upcoming().size)
        service.cancelFuture(1, "student-1")
        verify(repository).cancelFuture(eq(1), eq("student-1"), any())
    }

    @Test
    fun `replace loads and authorizes reminder`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Future", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(7200))
        whenever(reminders.findById(1)).thenReturn(java.util.Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.saveAll(any<List<Notification>>())).thenAnswer { invocation -> invocation.getArgument<List<Notification>>(0).onEachIndexed { index, n -> n.id = index + 1L } }

        assertEquals(1, service.replace(1, setOf(60)).size)
        verify(access).requireCanView(reminder)
    }

    @Test
    fun `missing reminder cannot list or replace notifications`() {
        whenever(reminders.findById(99)).thenReturn(java.util.Optional.empty())
        assertThrows(NotFoundException::class.java) { service.list(99) }
        assertThrows(NotFoundException::class.java) { service.replace(99, setOf(60)) }
    }

    @Test
    fun `drops notification offsets that are already in the past`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Soon", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(300))
        whenever(repository.saveAll(any<List<Notification>>())).thenReturn(emptyList())
        assertEquals(0, service.replaceFor(reminder, "student-1", setOf(60)).size)
        verify(repository).saveAll(emptyList())
    }

    @Test
    fun `marks cancelled notification in response`() {
        val reminder = Reminder(id = 1, ownerUserId = "student-1", createdByUserId = "student-1", title = "Future", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(7200))
        val notification = Notification(id = 5, reminder = reminder, targetUserId = "student-1", notifyAt = Instant.now().plusSeconds(3600), cancelledAt = Instant.now())
        whenever(reminders.findById(1)).thenReturn(java.util.Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByReminderIdAndTargetUserIdOrderByNotifyAtAsc(1, "student-1")).thenReturn(listOf(notification))
        assertEquals(true, service.list(1).single().cancelled)
    }
}
