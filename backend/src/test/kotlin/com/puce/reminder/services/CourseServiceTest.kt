package com.puce.reminder.services

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.entities.Course
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
    private lateinit var currentUser: CurrentUser
    private lateinit var service: CourseService

    @BeforeEach
    fun setUp() {
        courses = mock(CourseRepository::class.java)
        currentUser = mock(CurrentUser::class.java)
        service = CourseService(courses, currentUser)
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
}
