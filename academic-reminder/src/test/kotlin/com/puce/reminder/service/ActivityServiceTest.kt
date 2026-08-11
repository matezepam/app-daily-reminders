package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.ActivityRequest
import com.puce.reminder.entity.Activity
import com.puce.reminder.entity.ActivityCompletion
import com.puce.reminder.entity.Course
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.ActivityMapper
import com.puce.reminder.repository.ActivityCompletionRepository
import com.puce.reminder.repository.ActivityRepository
import com.puce.reminder.repository.CourseMembershipRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class ActivityServiceTest {
    private val repository = mock<ActivityRepository>()
    private val completions = mock<ActivityCompletionRepository>()
    private val memberships = mock<CourseMembershipRepository>()
    private val courses = mock<CourseService>()
    private val access = mock<AccessService>()
    private val currentUser = mock<CurrentUser>()
    private val audit = mock<AuditService>()
    private val service = ActivityService(repository, completions, memberships, courses, access, currentUser, ActivityMapper(), audit)
    private val course = Course(id = 1, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")

    @Test
    fun `professor creates activity`() {
        whenever(courses.find(1)).thenReturn(course)
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(repository.nextActivityNumber("professor-1")).thenReturn(1)
        whenever(repository.save(any<Activity>())).thenAnswer { it.getArgument<Activity>(0).apply { id = 2 } }

        val response = service.create(1, ActivityRequest("Final project", null, Instant.now().plusSeconds(3600)))

        assertEquals(2, response.id)
        assertEquals(1, response.activityNumber)
        verify(access).requireCanEdit(course)
        verify(audit).record(eq("professor-1"), eq("INSERT"), eq("Activity"), eq(2L), isNull(), any())
    }

    @Test
    fun `duplicate completion returns conflict`() {
        val activity = Activity(course, "Final project", null, Instant.now().plusSeconds(3600), "professor-1", id = 2)
        whenever(repository.findById(2)).thenReturn(java.util.Optional.of(activity))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "student-1")).thenReturn(true)
        whenever(completions.existsByActivityIdAndStudentUserId(2, "student-1")).thenReturn(true)
        assertThrows(ConflictException::class.java) { service.complete(2) }
    }

    @Test
    fun `lists activities with per student completion`() {
        val activity = Activity(course, "Final project", null, Instant.now().plusSeconds(3600), "professor-1", id = 2)
        whenever(courses.find(1)).thenReturn(course)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByCourseIdOrderByDueAtAsc(1)).thenReturn(listOf(activity))
        val completedAt = Instant.now().minusSeconds(60)
        whenever(completions.findAllForCourseAndStudent(1, "student-1"))
            .thenReturn(listOf(ActivityCompletion(activity, "student-1", id = 8, completedAt = completedAt)))
        val response = service.list(1).single()
        assertTrue(response.completed)
        assertEquals(completedAt, response.completedAt)
        verify(access).requireCanView(course)
    }

    @Test
    fun `updates and deletes owned activity`() {
        val activity = Activity(course, "Old title", null, Instant.now().plusSeconds(3600), "professor-1", id = 2)
        whenever(repository.findById(2)).thenReturn(java.util.Optional.of(activity))
        whenever(currentUser.id()).thenReturn("professor-1")
        val updated = service.update(2, ActivityRequest("New title", "Changed", Instant.now().plusSeconds(7200)))
        assertEquals("New title", updated.title)
        service.delete(2)
        verify(repository).delete(activity)
        verify(audit).record(eq("professor-1"), eq("DELETE"), eq("Activity"), eq(2L), any(), isNull())
    }

    @Test
    fun `student completes activity once`() {
        val activity = Activity(course, "Final project", null, Instant.now().plusSeconds(3600), "professor-1", id = 2)
        whenever(repository.findById(2)).thenReturn(java.util.Optional.of(activity))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "student-1")).thenReturn(true)
        whenever(completions.existsByActivityIdAndStudentUserId(2, "student-1")).thenReturn(false)
        whenever(completions.save(any())).thenAnswer { it.getArgument<ActivityCompletion>(0).apply { id = 6 } }
        val response = service.complete(2)
        assertTrue(response.completed)
        assertEquals(true, response.completedAt != null)
        verify(audit).record(eq("student-1"), eq("INSERT"), eq("ActivityCompletion"), eq(6L), isNull(), any())
    }

    @Test
    fun `non enrolled user cannot complete activity`() {
        val activity = Activity(course, "Final project", null, Instant.now().plusSeconds(3600), "professor-1", id = 2)
        whenever(repository.findById(2)).thenReturn(java.util.Optional.of(activity))
        whenever(currentUser.id()).thenReturn("outsider")
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "outsider")).thenReturn(false)
        assertThrows(ForbiddenException::class.java) { service.complete(2) }
    }

    @Test
    fun `missing activity returns not found`() {
        whenever(repository.findById(99)).thenReturn(java.util.Optional.empty())
        assertThrows(NotFoundException::class.java) { service.delete(99) }
    }
}
