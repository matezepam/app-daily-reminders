package com.puce.reminder.mapper

import com.puce.reminder.entity.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class DomainMappersTest {
    private val course = Course(id = 1, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")

    @Test
    fun `maps activity`() {
        val activity = Activity(course, "Final project", null, Instant.now().plusSeconds(3600), "professor-1", activityNumber = 1, id = 2)
        val response = ActivityMapper().toResponse(activity)
        assertEquals(2, response.id)
        assertEquals(1, response.activityNumber)
        assertFalse(response.completed)
        assertNull(response.completedAt)
    }

    @Test
    fun `maps attendance`() {
        val attendance = Attendance(course, "student-1", LocalDate.now(), AttendanceStatus.PRESENT, "professor-1", id = 3)
        val response = AttendanceMapper().toResponse(attendance)
        assertEquals("student-1", response.studentUserId)
        assertEquals(AttendanceStatus.PRESENT, response.status)
    }

    @Test
    fun `maps course ownership and member count`() {
        val response = CourseMapper().toResponse(course, memberCount = 4, ownedByCurrentUser = true)

        assertEquals(1, response.id)
        assertEquals(4, response.memberCount)
        assertTrue(response.ownedByMe)
    }

    @Test
    fun `maps category and notification state`() {
        val category = PriorityCategory(id = 7, ownerUserId = "student-1", name = "Critical", color = "#AA0000", sortOrder = 2)
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val notification = Notification(id = 9, reminder = reminder, targetUserId = "student-1", notifyAt = Instant.now().plusSeconds(1800), cancelledAt = Instant.now())

        assertEquals("Critical", PriorityCategoryMapper().toResponse(category).name)
        val response = NotificationMapper().toResponse(notification)
        assertEquals(8, response.reminderId)
        assertTrue(response.cancelled)
        assertFalse(response.sent)
    }

    @Test
    fun `maps personal and course reminder scopes`() {
        val mapper = ReminderMapper()
        val personal = Reminder(id = 10, ownerUserId = "student-1", createdByUserId = "student-1", title = "Personal", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val courseReminder = Reminder(id = 11, course = course, createdByUserId = "professor-1", title = "Course", type = ReminderType.EXAM, dueAt = Instant.now().plusSeconds(7200))

        val personalResponse = mapper.toResponse(personal, ReminderStatus.PENDING, null, true, emptyList())
        val courseResponse = mapper.toResponse(courseReminder, ReminderStatus.COMPLETED, null, false, emptyList())

        assertTrue(personalResponse.personal)
        assertNull(personalResponse.courseId)
        assertFalse(courseResponse.personal)
        assertEquals("Architecture", courseResponse.courseName)
        assertEquals(ReminderStatus.COMPLETED, courseResponse.status)
    }
}
