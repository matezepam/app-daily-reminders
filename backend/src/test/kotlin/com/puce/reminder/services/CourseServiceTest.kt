package com.puce.reminder.services

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.entities.Course
import com.puce.reminder.entities.CourseMembership
import com.puce.reminder.repositories.CourseMembershipRepository
import com.puce.reminder.repositories.CourseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class CourseServiceTest {
    private lateinit var courses: CourseRepository
    private lateinit var memberships: CourseMembershipRepository
    private lateinit var currentUser: CurrentUser
    private lateinit var service: CourseService

    @BeforeEach
    fun setUp() {
        courses = mock(CourseRepository::class.java)
        memberships = mock(CourseMembershipRepository::class.java)
        currentUser = mock(CurrentUser::class.java)
        service = CourseService(courses, memberships, currentUser)
        `when`(currentUser.id()).thenReturn("teacher-id")
        `when`(courses.save(any(Course::class.java))).thenAnswer { invocation ->
            invocation.getArgument<Course>(0).apply { id = 1L }
        }
    }

    @Test
    fun `generates a shareable code with the course prefix`() {
        `when`(courses.existsByJoinCode(anyString())).thenReturn(false)

        val result = service.create(CourseCreateRequest("Matemáticas II", "Periodo actual"))

        assertTrue(result.joinCode.matches(Regex("MAT-[A-Z2-9]{5}")))
        assertEquals("teacher-id", result.ownerUserId)
    }

    @Test
    fun `retries when a generated code already exists`() {
        `when`(courses.existsByJoinCode(anyString())).thenReturn(true, false)

        service.create(CourseCreateRequest("Programación"))

        verify(courses, times(2)).existsByJoinCode(anyString())
    }

    @Test
    fun `student joins the course identified by the normalized code`() {
        val course = Course(
            id = 9L,
            name = "Programación",
            joinCode = "PRO-82KLM",
            ownerUserId = "teacher-id",
        )
        `when`(currentUser.id()).thenReturn("student-id")
        `when`(courses.findByJoinCodeIgnoreCase("PRO-82KLM")).thenReturn(course)
        `when`(memberships.existsByCourseIdAndStudentUserId(9L, "student-id")).thenReturn(false)

        val result = service.join(" pro-82klm ")

        assertEquals(9L, result.id)
        verify(memberships).save(any(CourseMembership::class.java))
    }
}
