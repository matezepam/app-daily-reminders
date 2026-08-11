package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.client.UsersClient
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.AttendanceRequest
import com.puce.reminder.dto.ExternalUserResponse
import com.puce.reminder.entity.Attendance
import com.puce.reminder.entity.AttendanceStatus
import com.puce.reminder.entity.Course
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.mapper.AttendanceMapper
import com.puce.reminder.repository.AttendanceRepository
import com.puce.reminder.repository.CourseMembershipRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Instant
import java.time.LocalDate

class AttendanceServiceTest {
    private val repository = mock<AttendanceRepository>()
    private val memberships = mock<CourseMembershipRepository>()
    private val courses = mock<CourseService>()
    private val usersClient = mock<UsersClient>()
    private val access = mock<AccessService>()
    private val currentUser = mock<CurrentUser>()
    private val audit = mock<AuditService>()
    private val service = AttendanceService(repository, memberships, courses, usersClient, access, currentUser, AttendanceMapper(), audit)
    private val course = Course(id = 1, name = "Architecture", joinCode = "ARC-12345", professorUserId = "professor-1")

    @Test
    fun `records attendance for enrolled student`() {
        val date = LocalDate.now()
        whenever(courses.find(1)).thenReturn(course)
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "student-1")).thenReturn(true)
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(usersClient.getUser("student-1")).thenReturn(ExternalUserResponse(1, "student-1", "s@example.com", "Student", "STUDENT", Instant.now(), Instant.now()))
        whenever(repository.save(any<Attendance>())).thenAnswer { it.getArgument<Attendance>(0).apply { id = 5 } }

        val response = service.record(1, AttendanceRequest("student-1", date, AttendanceStatus.PRESENT))

        assertEquals(5, response.id)
        verify(audit).record(eq("professor-1"), eq("INSERT"), eq("Attendance"), eq(5L), isNull(), any())
    }

    @Test
    fun `rejects attendance for non member`() {
        whenever(courses.find(1)).thenReturn(course)
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "outsider")).thenReturn(false)
        assertThrows(BadRequestException::class.java) { service.record(1, AttendanceRequest("outsider", LocalDate.now(), AttendanceStatus.ABSENT)) }
    }

    @Test
    fun `updates existing attendance`() {
        val date = LocalDate.now()
        val existing = Attendance(course, "student-1", date, AttendanceStatus.ABSENT, "professor-1", id = 5)
        whenever(courses.find(1)).thenReturn(course)
        whenever(memberships.existsByCourseIdAndStudentUserId(1, "student-1")).thenReturn(true)
        whenever(currentUser.id()).thenReturn("professor-1")
        whenever(usersClient.getUser("student-1")).thenReturn(ExternalUserResponse(1, "student-1", "s@example.com", "Student", "STUDENT", Instant.now(), Instant.now()))
        whenever(repository.findByCourseIdAndStudentUserIdAndAttendanceDate(1, "student-1", date)).thenReturn(existing)
        whenever(repository.save(existing)).thenReturn(existing)

        val response = service.record(1, AttendanceRequest("student-1", date, AttendanceStatus.PRESENT))

        assertEquals(AttendanceStatus.PRESENT, response.status)
        verify(audit).record(
            eq("professor-1"), eq("UPDATE"), eq("Attendance"), eq(5L),
            argThat<String> { contains("ABSENT") }, argThat<String> { contains("PRESENT") },
        )
    }

    @Test
    fun `lists attendance after view authorization`() {
        val attendance = Attendance(course, "student-1", LocalDate.now(), AttendanceStatus.PRESENT, "professor-1", id = 5)
        whenever(courses.find(1)).thenReturn(course)
        whenever(access.canEdit(course)).thenReturn(true)
        whenever(repository.findAllByCourseIdOrderByAttendanceDateDescStudentUserId(1)).thenReturn(listOf(attendance))

        assertEquals(5, service.list(1).single().id)
        verify(access).requireCanView(course)
    }

    @Test
    fun `student only lists own attendance`() {
        val attendance = Attendance(course, "student-1", LocalDate.now(), AttendanceStatus.PRESENT, "professor-1", id = 5)
        whenever(courses.find(1)).thenReturn(course)
        whenever(access.canEdit(course)).thenReturn(false)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByCourseIdAndStudentUserIdOrderByAttendanceDateDesc(1, "student-1")).thenReturn(listOf(attendance))

        assertEquals(listOf("student-1"), service.list(1).map { it.studentUserId })
        verify(repository, never()).findAllByCourseIdOrderByAttendanceDateDescStudentUserId(1)
    }
}
