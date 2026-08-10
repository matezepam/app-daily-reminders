package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.client.UsersClient
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.entity.Course
import com.puce.reminder.entity.CourseMembership
import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.entity.ReminderType
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.CourseMapper
import com.puce.reminder.repository.CourseMembershipRepository
import com.puce.reminder.repository.CourseRepository
import com.puce.reminder.repository.ReminderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant

class CourseServiceTest {
    private val courses = mock<CourseRepository>()
    private val memberships = mock<CourseMembershipRepository>()
    private val reminders = mock<ReminderRepository>()
    private val notifications = mock<NotificationService>()
    private val access = mock<AccessService>()
    private val currentUser = mock<CurrentUser>()
    private val usersClient = mock<UsersClient>()
    private val audit = mock<AuditService>()
    private val service = CourseService(courses, memberships, reminders, notifications, access, currentUser, usersClient, audit, CourseMapper())

    @Test
    fun `course creation uses authenticated subject`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(courses.existsEquivalentCourse("professor-1", "Architecture", "Course")).thenReturn(false)
        whenever(courses.existsByJoinCode(any())).thenReturn(false)
        whenever(courses.save(any<Course>())).thenAnswer { it.getArgument<Course>(0).apply { id = 10 } }
        whenever(memberships.countByCourseId(10)).thenReturn(0)

        val response = service.create(CourseCreateRequest("Architecture", "Course"))

        assertEquals("professor-1", response.professorUserId)
        verify(audit).record(eq("professor-1"), eq("INSERT"), eq("Course"), eq(10L), isNull(), any())
    }

    @Test
    fun `rejects duplicate course name and description for same professor`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(courses.existsEquivalentCourse("professor-1", "Architecture", "Course")).thenReturn(true)

        val exception = assertThrows(ConflictException::class.java) {
            service.create(CourseCreateRequest("  Architecture  ", "  Course  "))
        }

        assertEquals(CourseService.DUPLICATE_COURSE_MESSAGE, exception.message)
        verify(courses, never()).existsByJoinCode(any())
        verify(courses, never()).save(any<Course>())
    }

    @Test
    fun `invalid join code returns not found`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(courses.findByJoinCodeIgnoreCase("BAD-CODE")).thenReturn(null)
        assertThrows(com.puce.reminder.exception.NotFoundException::class.java) { service.join("bad-code") }
    }

    @Test
    fun `student joins course once`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(courses.findByJoinCodeIgnoreCase("ARC-12345")).thenReturn(course)
        whenever(memberships.existsByCourseIdAndStudentUserId(3, "student-1")).thenReturn(false)
        whenever(reminders.findAllByCourseIdOrderByDueAtAsc(3)).thenReturn(emptyList())
        whenever(memberships.countByCourseId(3)).thenReturn(1)
        val response = service.join("arc-12345")
        assertEquals(3, response.id)
        verify(memberships).save(argThat { studentUserId == "student-1" })
    }

    @Test
    fun `lists owned and joined courses without duplicates`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(courses.findAllByProfessorUserIdOrderByCreatedAtDesc("professor-1")).thenReturn(listOf(course))
        whenever(memberships.findAllByStudentUserIdOrderByJoinedAtDesc("professor-1")).thenReturn(emptyList())
        whenever(memberships.countByCourseId(3)).thenReturn(0)
        assertTrue(service.mine().single().ownedByMe)
    }

    @Test
    fun `loads professor through users service`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(courses.findById(3)).thenReturn(java.util.Optional.of(course))
        val expected = com.puce.reminder.dto.ExternalUserResponse(1, "professor-1", "p@example.com", "Professor", "ADMIN", java.time.Instant.now(), java.time.Instant.now())
        whenever(usersClient.getUser("professor-1")).thenReturn(expected)
        assertEquals(expected, service.professor(3))
        verify(access).requireCanView(course)
    }

    @Test
    fun `lists enrolled students for course owner`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val membership = CourseMembership(id = 4, course = course, studentUserId = "student-1")
        val expected = com.puce.reminder.dto.ExternalUserResponse(1, "student-1", "s@example.com", "Student", "STUDENT", Instant.now(), Instant.now())
        whenever(courses.findById(3)).thenReturn(java.util.Optional.of(course))
        whenever(memberships.findAllByCourseId(3)).thenReturn(listOf(membership))
        whenever(usersClient.getUser("student-1")).thenReturn(expected)

        assertEquals(listOf(expected), service.students(3))
        verify(access).requireCanEdit(course)
    }

    @Test
    fun `deletes course after ownership check`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(courses.findById(3)).thenReturn(java.util.Optional.of(course))
        whenever(currentUser.id()).thenReturn("professor-1")
        service.delete(3)
        verify(access).requireCanEdit(course)
        verify(courses).delete(course)
    }

    @Test
    fun `gets visible course`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(courses.findById(3)).thenReturn(java.util.Optional.of(course))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.countByCourseId(3)).thenReturn(2)

        val response = service.get(3)

        assertEquals(2, response.memberCount)
        verify(access).requireCanView(course)
    }

    @Test
    fun `professor and existing member cannot join course`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        whenever(courses.findByJoinCodeIgnoreCase("ARC-12345")).thenReturn(course)
        whenever(currentUser.id()).thenReturn("professor-1")
        assertThrows(ConflictException::class.java) { service.join("ARC-12345") }

        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(3, "student-1")).thenReturn(true)
        assertThrows(ConflictException::class.java) { service.join("ARC-12345") }
    }

    @Test
    fun `joining course schedules only pending future reminders`() {
        val course = Course(id = 3, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val future = Reminder(id = 7, course = course, createdByUserId = "professor-1", title = "Future", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val completed = Reminder(id = 8, course = course, createdByUserId = "professor-1", title = "Done", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600), status = ReminderStatus.COMPLETED)
        val expired = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Expired", type = ReminderType.TASK, dueAt = Instant.now().minusSeconds(60))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(courses.findByJoinCodeIgnoreCase("ARC-12345")).thenReturn(course)
        whenever(memberships.existsByCourseIdAndStudentUserId(3, "student-1")).thenReturn(false)
        whenever(reminders.findAllByCourseIdOrderByDueAtAsc(3)).thenReturn(listOf(future, completed, expired))

        service.join("ARC-12345")

        verify(notifications).replaceFor(future, "student-1", setOf(60))
        verify(notifications, never()).replaceFor(completed, "student-1", setOf(60))
        verify(notifications, never()).replaceFor(expired, "student-1", setOf(60))
    }

    @Test
    fun `missing course returns not found`() {
        whenever(courses.findById(99)).thenReturn(java.util.Optional.empty())
        assertThrows(NotFoundException::class.java) { service.find(99) }
    }

    @Test
    fun `fails when no unique join code can be generated`() {
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(courses.existsByJoinCode(any())).thenReturn(true)
        assertThrows(ConflictException::class.java) { service.create(CourseCreateRequest("A", null)) }
        verify(courses, never()).save(any<Course>())
    }
}
