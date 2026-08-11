package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.ReminderRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.dto.NotificationResponse
import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.ReminderPriority
import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.entity.ReminderType
import com.puce.reminder.entity.Course
import com.puce.reminder.entity.CourseMembership
import com.puce.reminder.entity.PriorityCategory
import com.puce.reminder.entity.StudentReminderState
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.mapper.PriorityCategoryMapper
import com.puce.reminder.mapper.ReminderMapper
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.repository.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional

class ReminderServiceTest {
    private val reminders = mock<ReminderRepository>()
    private val memberships = mock<CourseMembershipRepository>()
    private val states = mock<StudentReminderStateRepository>()
    private val categories = mock<PriorityCategoryRepository>()
    private val courses = mock<CourseService>()
    private val notifications = mock<NotificationService>()
    private val access = mock<AccessService>()
    private val currentUser = mock<CurrentUser>()
    private val audit = mock<AuditService>()
    private val categoryMapper = PriorityCategoryMapper()
    private val service = ReminderService(reminders, memberships, states, categories, courses, notifications, access, currentUser, audit, ReminderMapper(), categoryMapper)

    @Test
    fun `creates personal reminder from authenticated subject`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(reminders.save(any<Reminder>())).thenAnswer { it.getArgument<Reminder>(0).apply { id = 8 } }
        val response = service.createPersonal(request())
        assertEquals(8, response.id)
        assertEquals(true, response.personal)
        verify(audit).record(eq("student-1"), eq("INSERT"), eq("Reminder"), eq(8L), isNull(), any())
    }

    @Test
    fun `completes own personal reminder`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findByIdForUpdate(8)).thenReturn(reminder)
        whenever(reminders.save(reminder)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(notifications.list(8)).thenReturn(emptyList())
        val response = service.complete(8)
        assertEquals(ReminderStatus.COMPLETED, response.status)
        verify(notifications).cancelFuture(8, "student-1")
    }

    @Test
    fun `reopens personal reminder and restores its future notification schedule`() {
        val dueAt = Instant.now().plusSeconds(3600)
        val reminder = Reminder(
            id = 8,
            ownerUserId = "student-1",
            createdByUserId = "student-1",
            title = "Review",
            type = ReminderType.TASK,
            dueAt = dueAt,
            status = ReminderStatus.COMPLETED,
        )
        whenever(reminders.findByIdForUpdate(8)).thenReturn(reminder)
        whenever(reminders.save(reminder)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(notifications.list(8)).thenReturn(
            listOf(NotificationResponse(4, 8, dueAt.minusSeconds(600), sent = false, cancelled = true)),
        )

        val response = service.uncomplete(8)

        assertEquals(ReminderStatus.PENDING, response.status)
        assertEquals(null, response.completedAt)
        verify(notifications).replaceFor(reminder, "student-1", setOf(10))
    }

    @Test
    fun `expired reminder cannot be reopened`() {
        val reminder = Reminder(
            id = 8,
            ownerUserId = "student-1",
            createdByUserId = "student-1",
            title = "Review",
            type = ReminderType.TASK,
            dueAt = Instant.now().minusSeconds(1),
            status = ReminderStatus.COMPLETED,
        )
        whenever(reminders.findByIdForUpdate(8)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")

        assertThrows(BadRequestException::class.java) { service.uncomplete(8) }
    }

    @Test
    fun `student reopens completed course reminder`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val state = StudentReminderState(id = 12, reminder = reminder, studentUserId = "student-1", completedAt = Instant.now())
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "student-1")).thenReturn(true)
        whenever(states.findByReminderIdAndStudentUserId(9, "student-1")).thenReturn(state)
        whenever(states.save(state)).thenReturn(state)
        whenever(notifications.list(9)).thenReturn(emptyList())

        val response = service.uncomplete(9)

        assertEquals(ReminderStatus.PENDING, response.status)
        assertEquals(null, state.completedAt)
        verify(states).save(state)
        verify(notifications).replaceFor(reminder, "student-1", emptySet())
    }

    @Test
    fun `non member cannot reopen course reminder`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("outsider")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "outsider")).thenReturn(false)
        whenever(notifications.list(9)).thenReturn(emptyList())

        assertThrows(ForbiddenException::class.java) { service.uncomplete(9) }
    }

    @Test
    fun `course reminder without completion state cannot be reopened`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "student-1")).thenReturn(true)
        whenever(states.findByReminderIdAndStudentUserId(9, "student-1")).thenReturn(null)
        whenever(notifications.list(9)).thenReturn(emptyList())

        assertThrows(com.puce.reminder.exception.ConflictException::class.java) { service.uncomplete(9) }
    }

    @Test
    fun `course reminder with pending state cannot be reopened`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val state = StudentReminderState(id = 12, reminder = reminder, studentUserId = "student-1", completedAt = null)
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "student-1")).thenReturn(true)
        whenever(states.findByReminderIdAndStudentUserId(9, "student-1")).thenReturn(state)
        whenever(notifications.list(9)).thenReturn(emptyList())

        assertThrows(com.puce.reminder.exception.ConflictException::class.java) { service.uncomplete(9) }
    }

    @Test
    fun `updates and deletes personal reminder`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Old", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(8)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(notifications.list(8)).thenReturn(emptyList())
        val response = service.update(8, request().copy(title = "Updated"))
        assertEquals("Updated", response.title)
        verify(notifications).replaceFor(eq(reminder), eq("student-1"), eq(setOf(60)))
        service.delete(8)
        verify(reminders).delete(reminder)
    }

    @Test
    fun `lists personal reminders and builds dashboard`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(currentUser.isAdmin()).thenReturn(false)
        whenever(reminders.findVisibleToStudent("student-1")).thenReturn(listOf(reminder))
        whenever(notifications.list(8)).thenReturn(emptyList())
        assertEquals(1, service.mine().size)
        assertEquals(1, service.dashboard().upcoming)
    }

    @Test
    fun `creates and lists course reminder`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val member = CourseMembership(id = 3, course = course, studentUserId = "student-1")
        whenever(courses.find(2)).thenReturn(course)
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(memberships.findAllByCourseId(2)).thenReturn(listOf(member))
        whenever(reminders.save(any<Reminder>())).thenAnswer { it.getArgument<Reminder>(0).apply { id = 9 } }
        val created = service.createForCourse(2, request())
        assertEquals(2, created.courseId)
        whenever(reminders.findAllByCourseIdOrderByDueAtAsc(2)).thenReturn(listOf(Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))))
        assertEquals(1, service.byCourse(2).size)
    }

    @Test
    fun `personal reminder rejects course priority override`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(8)).thenReturn(Optional.of(reminder))
        assertThrows(BadRequestException::class.java) { service.setPriorityOverride(8, null) }
    }

    @Test
    fun `professor lists personal and owned course reminders without duplicates`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val personal = Reminder(id = 8, ownerUserId = "professor-1", createdByUserId = "professor-1", title = "Personal", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val shared = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Shared", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(7200))
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(currentUser.isAdmin()).thenReturn(true)
        whenever(courses.mine()).thenReturn(listOf(CourseResponse(2, "Architecture", null, "ARC-12345", "professor-1", 1, Instant.now(), true)))
        whenever(reminders.findAllByOwnerUserIdOrderByDueAtAsc("professor-1")).thenReturn(listOf(personal))
        whenever(reminders.findAllByCourseIdOrderByDueAtAsc(2)).thenReturn(listOf(shared))
        whenever(notifications.list(any())).thenReturn(emptyList())

        assertEquals(listOf(8L, 9L), service.mine().map { it.id })
    }

    @Test
    fun `course completion creates per student state`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        var savedState: StudentReminderState? = null
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "student-1")).thenReturn(true)
        whenever(states.findByReminderIdAndStudentUserId(9, "student-1")).thenAnswer { savedState }
        whenever(states.save(any<StudentReminderState>())).thenAnswer { invocation ->
            invocation.getArgument<StudentReminderState>(0).apply { id = 12 }.also { savedState = it }
        }
        whenever(notifications.list(9)).thenReturn(emptyList())

        val response = service.complete(9)

        assertEquals(ReminderStatus.COMPLETED, response.status)
        verify(states).save(any<StudentReminderState>())
    }

    @Test
    fun `non member cannot complete course reminder`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findByIdForUpdate(9)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("outsider")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "outsider")).thenReturn(false)
        assertThrows(ForbiddenException::class.java) { service.complete(9) }
    }

    @Test
    fun `non owner cannot complete personal reminder`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findByIdForUpdate(8)).thenReturn(reminder)
        whenever(currentUser.id()).thenReturn("student-2")
        assertThrows(ForbiddenException::class.java) { service.complete(8) }
    }

    @Test
    fun `student sets custom priority for course reminder`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val category = PriorityCategory(id = 4, ownerUserId = "student-1", name = "Critical", color = "#AA0000")
        var savedState: StudentReminderState? = null
        whenever(reminders.findById(9)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "student-1")).thenReturn(true)
        whenever(states.findByReminderIdAndStudentUserId(9, "student-1")).thenAnswer { savedState }
        whenever(states.save(any<StudentReminderState>())).thenAnswer { invocation ->
            invocation.getArgument<StudentReminderState>(0).also { savedState = it }
        }
        whenever(categories.findByIdAndOwnerUserId(4, "student-1")).thenReturn(category)
        whenever(states.save(any<StudentReminderState>())).thenAnswer { invocation ->
            invocation.getArgument<StudentReminderState>(0).apply { id = 12 }.also { savedState = it }
        }

        val response = service.setPriorityOverride(9, 4)

        assertEquals(4, response.customPriority?.id)
        verify(states).save(any<StudentReminderState>())
        verify(audit).record(
            eq("student-1"), eq("UPDATE"), eq("StudentReminderState"), eq(12L), isNull(),
            argThat<String> { contains("priorityCategoryId") },
        )
    }

    @Test
    fun `non member cannot customize course reminder priority`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(9)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("outsider")
        whenever(memberships.existsByCourseIdAndStudentUserId(2, "outsider")).thenReturn(false)
        assertThrows(ForbiddenException::class.java) { service.setPriorityOverride(9, null) }
    }

    @Test
    fun `course update replaces schedules for every member`() {
        val course = Course(id = 2, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")
        val reminder = Reminder(id = 9, course = course, createdByUserId = "professor-1", title = "Old", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        val first = CourseMembership(id = 1, course = course, studentUserId = "student-1")
        val second = CourseMembership(id = 2, course = course, studentUserId = "student-2")
        whenever(reminders.findById(9)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(memberships.findAllByCourseId(2)).thenReturn(listOf(first, second))
        whenever(notifications.list(9)).thenReturn(emptyList())

        val response = service.update(9, request().copy(title = "Updated", description = " "))

        assertEquals(null, response.description)
        verify(notifications).replaceFor(reminder, "student-1", setOf(60))
        verify(notifications).replaceFor(reminder, "student-2", setOf(60))
    }

    @Test
    fun `missing reminder and missing category return not found`() {
        whenever(reminders.findById(99)).thenReturn(Optional.empty())
        assertThrows(NotFoundException::class.java) { service.get(99) }

        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(8)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("student-1")
        assertThrows(NotFoundException::class.java) { service.update(8, request().copy(priorityCategoryId = 44)) }
    }

    @Test
    fun `dashboard counts pending today and completed this month`() {
        val today = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Today", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(60))
        val completed = Reminder(id = 9, ownerUserId = "student-1", createdByUserId = "student-1", title = "Done", type = ReminderType.TASK, dueAt = Instant.now().minusSeconds(60), status = ReminderStatus.COMPLETED)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(currentUser.isAdmin()).thenReturn(false)
        whenever(reminders.findVisibleToStudent("student-1")).thenReturn(listOf(today, completed))
        whenever(notifications.list(any())).thenReturn(emptyList())

        val dashboard = service.dashboard()

        assertEquals(1, dashboard.pendingToday)
        assertEquals(1, dashboard.completedThisMonth)
    }

    @Test
    fun `professor cannot edit another users reminder response`() {
        val reminder = Reminder(id = 8, ownerUserId = "student-1", createdByUserId = "student-1", title = "Review", type = ReminderType.TASK, dueAt = Instant.now().plusSeconds(3600))
        whenever(reminders.findById(8)).thenReturn(Optional.of(reminder))
        whenever(currentUser.id()).thenReturn("admin-1")
        whenever(currentUser.hasRole("ADMIN")).thenReturn(true)
        whenever(notifications.list(8)).thenReturn(emptyList())
        assertEquals(false, service.get(8).editable)
    }

    private fun request() = ReminderRequest("Review", "Prepare", ReminderType.TASK, Instant.now().plusSeconds(3600), ReminderPriority.HIGH, notificationOffsetsMinutes = setOf(60))
}
