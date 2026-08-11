package com.puce.reminder.service

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.entity.Course
import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.ReminderType
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.repository.CourseMembershipRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class AccessServiceTest {
    private val currentUser = mock<CurrentUser>()
    private val memberships = mock<CourseMembershipRepository>()
    private val service = AccessService(currentUser, memberships)
    private val course = Course(id = 1, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")

    @Test
    fun `course professor can view and edit`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        assertTrue(service.canView(course))
        assertTrue(service.canEdit(course))
    }

    @Test
    fun `enrolled student can view but cannot edit`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "student-1")).thenReturn(true)
        assertTrue(service.canView(course))
        assertFalse(service.canEdit(course))
        assertThrows(ForbiddenException::class.java) { service.requireCanEdit(course) }
    }

    @Test
    fun `outsider cannot view course`() {
        whenever(currentUser.id()).thenReturn("outsider")
        assertThrows(ForbiddenException::class.java) { service.requireCanView(course) }
    }

    @Test
    fun `user cannot edit reminder owned by another user`() {
        whenever(currentUser.id()).thenReturn("student-2")
        val reminder = Reminder(ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        assertThrows(ForbiddenException::class.java) { service.requireCanEdit(reminder) }
    }

    @Test
    fun `professor cannot view or edit another professors course`() {
        whenever(currentUser.id()).thenReturn("professor-2")
        whenever(currentUser.hasRole("ADMIN")).thenReturn(true)
        assertFalse(service.canView(course))
        assertFalse(service.canEdit(course))
        assertThrows(ForbiddenException::class.java) { service.requireCanView(course) }
        assertThrows(ForbiddenException::class.java) { service.requireCanEdit(course) }
    }

    @Test
    fun `course reminder delegates view and edit checks to course permissions`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        val reminder = Reminder(course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        service.requireCanView(reminder)
        service.requireCanEdit(reminder)
    }

    @Test
    fun `owner can view and edit personal reminder`() {
        whenever(currentUser.id()).thenReturn("student-1")
        val reminder = Reminder(ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        service.requireCanView(reminder)
        service.requireCanEdit(reminder)
    }

    @Test
    fun `user cannot view personal reminder owned by another user`() {
        whenever(currentUser.id()).thenReturn("student-2")
        whenever(currentUser.hasRole("ADMIN")).thenReturn(false)
        val reminder = Reminder(ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        assertThrows(ForbiddenException::class.java) { service.requireCanView(reminder) }
    }

    @Test
    fun `professor cannot view another users personal reminder`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(currentUser.hasRole("ADMIN")).thenReturn(true)
        val reminder = Reminder(ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        assertThrows(ForbiddenException::class.java) { service.requireCanView(reminder) }
        assertThrows(ForbiddenException::class.java) { service.requireCanEdit(reminder) }
    }
}
