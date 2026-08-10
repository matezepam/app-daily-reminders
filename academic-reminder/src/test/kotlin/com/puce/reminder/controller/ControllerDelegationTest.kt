package com.puce.reminder.controller

import com.puce.reminder.dto.*
import com.puce.reminder.entity.*
import com.puce.reminder.service.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate

class ControllerDelegationTest {
    private val now = Instant.parse("2030-01-01T00:00:00Z")
    private val future = Instant.parse("2035-01-01T00:00:00Z")

    @Test
    fun `course controller delegates every endpoint`() {
        val service = mock<CourseService>()
        val controller = CourseController(service)
        val request = CourseCreateRequest("Architecture", "Course")
        val response = courseResponse()
        val professor = ExternalUserResponse(2, "professor-1", "p@example.com", "Professor", "ADMIN", now, now)
        whenever(service.create(request)).thenReturn(response)
        whenever(service.mine()).thenReturn(listOf(response))
        whenever(service.get(1)).thenReturn(response)
        whenever(service.professor(1)).thenReturn(professor)
        whenever(service.students(1)).thenReturn(emptyList())
        whenever(service.join("ARC-12345")).thenReturn(response)

        assertEquals(1, controller.create(request).id)
        assertEquals(1, controller.mine().single().id)
        assertEquals(1, controller.get(1).id)
        assertEquals("ADMIN", controller.professor(1).role)
        assertTrue(controller.students(1).isEmpty())
        assertEquals(1, controller.join(JoinCourseRequest("ARC-12345")).id)
        controller.delete(1)
        verify(service).delete(1)
    }

    @Test
    fun `activity controller delegates every endpoint`() {
        val service = mock<ActivityService>()
        val controller = ActivityController(service)
        val request = ActivityRequest("Defense", "Present", future)
        val response = activityResponse()
        whenever(service.create(1, request)).thenReturn(response)
        whenever(service.list(1)).thenReturn(listOf(response))
        whenever(service.update(2, request)).thenReturn(response)
        whenever(service.complete(2)).thenReturn(response.copy(completed = true))

        assertEquals(2, controller.create(1, request).id)
        assertEquals(2, controller.list(1).single().id)
        assertEquals("Defense", controller.update(2, request).title)
        assertTrue(controller.complete(2).completed)
        controller.delete(2)
        verify(service).delete(2)
    }

    @Test
    fun `attendance controller delegates every endpoint`() {
        val service = mock<AttendanceService>()
        val controller = AttendanceController(service)
        val request = AttendanceRequest("student-1", LocalDate.of(2030, 1, 1), AttendanceStatus.PRESENT)
        val response = AttendanceResponse(3, 1, "student-1", request.attendanceDate, AttendanceStatus.PRESENT, "professor-1", now, now)
        whenever(service.record(1, request)).thenReturn(response)
        whenever(service.list(1)).thenReturn(listOf(response))

        assertEquals(3, controller.record(1, request).id)
        assertEquals(AttendanceStatus.PRESENT, controller.list(1).single().status)
    }

    @Test
    fun `priority category controller delegates every endpoint`() {
        val service = mock<PriorityCategoryService>()
        val controller = PriorityCategoryController(service)
        val request = PriorityCategoryRequest("Critical", "#AA0000", 1)
        val response = PriorityCategoryResponse(4, "Critical", "#AA0000", 1)
        whenever(service.create(request)).thenReturn(response)
        whenever(service.list()).thenReturn(listOf(response))

        assertEquals(4, controller.create(request).id)
        assertEquals("Critical", controller.list().single().name)
        controller.delete(4)
        verify(service).delete(4)
    }

    @Test
    fun `notification controller delegates every endpoint`() {
        val service = mock<NotificationService>()
        val controller = NotificationController(service)
        val response = NotificationResponse(5, 6, future, sent = false, cancelled = false)
        whenever(service.list(6)).thenReturn(listOf(response))
        whenever(service.replace(6, setOf(60))).thenReturn(listOf(response))
        whenever(service.upcoming()).thenReturn(listOf(response))

        assertEquals(5, controller.list(6).single().id)
        assertEquals(5, controller.replace(6, NotificationRequest(setOf(60))).single().id)
        assertEquals(5, controller.upcoming().single().id)
        controller.delete(5)
        verify(service).delete(5)
    }

    @Test
    fun `reminder controller delegates every endpoint`() {
        val service = mock<ReminderService>()
        val controller = ReminderController(service)
        val request = ReminderRequest("Review", "Prepare", ReminderType.TASK, future, ReminderPriority.HIGH, null, setOf(60))
        val response = reminderResponse()
        whenever(service.createPersonal(request)).thenReturn(response)
        whenever(service.createForCourse(1, request)).thenReturn(response.copy(courseId = 1, personal = false))
        whenever(service.mine()).thenReturn(listOf(response))
        whenever(service.dashboard()).thenReturn(DashboardResponse(0, 1, 0, listOf(response)))
        whenever(service.byCourse(1)).thenReturn(listOf(response.copy(courseId = 1, personal = false)))
        whenever(service.get(6)).thenReturn(response)
        whenever(service.update(6, request)).thenReturn(response.copy(title = "Updated"))
        whenever(service.complete(6)).thenReturn(response.copy(status = ReminderStatus.COMPLETED))
        whenever(service.setPriorityOverride(6, 4)).thenReturn(response.copy(customPriority = PriorityCategoryResponse(4, "Critical", "#AA0000", 1)))

        assertEquals(6, controller.createPersonal(request).id)
        assertEquals(1, controller.createForCourse(1, request).courseId)
        assertEquals(6, controller.mine().single().id)
        assertEquals(1, controller.dashboard().upcoming)
        assertEquals(1, controller.byCourse(1).single().courseId)
        assertEquals(6, controller.get(6).id)
        assertEquals("Updated", controller.update(6, request).title)
        assertEquals(ReminderStatus.COMPLETED, controller.complete(6).status)
        assertEquals(4, controller.overridePriority(6, ReminderPriorityOverrideRequest(4)).customPriority?.id)
        controller.delete(6)
        verify(service).delete(6)
    }

    private fun courseResponse() = CourseResponse(1, "Architecture", "Course", "ARC-12345", "professor-1", 0, now, true)

    private fun activityResponse() = ActivityResponse(2, 1, "Defense", "Present", future, "professor-1", false, now, now)

    private fun reminderResponse() = ReminderResponse(
        id = 6,
        courseId = null,
        courseName = null,
        personal = true,
        title = "Review",
        description = "Prepare",
        type = ReminderType.TASK,
        dueAt = future,
        priority = ReminderPriority.HIGH,
        customPriority = null,
        status = ReminderStatus.PENDING,
        editable = true,
        createdAt = now,
    )
}
