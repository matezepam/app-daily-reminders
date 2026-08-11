package com.puce.reminder.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataAccessException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import com.puce.reminder.service.CourseService

class RestExceptionHandlerTest {
    private val handler = RestExceptionHandler()
    private val request = MockHttpServletRequest("GET", "/academic-reminder/courses/99")

    @Test
    fun `maps domain exceptions to consistent http errors`() {
        val notFound = handler.notFound(NotFoundException("Course not found"), request)
        val conflict = handler.conflict(ConflictException("Enrollment already exists"), request)
        val unavailable = handler.serviceUnavailable(ServiceUnavailableException("Users service is unavailable"), request)
        val dataConflict = handler.dataConflict(DataIntegrityViolationException("private database detail"), request)

        assertEquals(404, notFound.statusCode.value())
        assertEquals("Course not found", notFound.body?.message)
        assertEquals("/academic-reminder/courses/99", notFound.body?.path)
        assertEquals(409, conflict.statusCode.value())
        assertEquals("The requested data conflicts with an existing record", dataConflict.body?.message)
        assertEquals(503, unavailable.statusCode.value())
    }

    @Test
    fun `maps remaining domain and security exceptions`() {
        assertEquals(403, handler.forbidden(ForbiddenException("Forbidden"), request).statusCode.value())
        assertEquals(403, handler.accessDenied(AccessDeniedException("Denied"), request).statusCode.value())
        assertEquals(400, handler.badRequest(BadRequestException("Bad input"), request).statusCode.value())
        assertEquals(502, handler.badGateway(BadGatewayException("Bad gateway"), request).statusCode.value())
    }

    @Test
    fun `returns specific conflict for concurrent duplicate course`() {
        val cause = IllegalStateException(
            "duplicate key value violates unique constraint uk_courses_professor_name_description",
        )
        val response = handler.dataConflict(DataIntegrityViolationException("insert failed", cause), request)

        assertEquals(409, response.statusCode.value())
        assertEquals(CourseService.DUPLICATE_COURSE_MESSAGE, response.body?.message)
    }

    @Test
    fun `maps field validation errors`() {
        val exception = mock<MethodArgumentNotValidException>()
        val bindingResult = mock<BindingResult>()
        whenever(exception.bindingResult).thenReturn(bindingResult)
        whenever(bindingResult.fieldErrors).thenReturn(listOf(FieldError("request", "name", "must not be blank")))

        val response = handler.validation(exception, request)

        assertEquals(400, response.statusCode.value())
        assertEquals("must not be blank", response.body?.fieldErrors?.get("name"))
    }

    @Test
    fun `hides unexpected server error details`() {
        val response = handler.unexpected(IllegalStateException("database details"), request)

        assertEquals(500, response.statusCode.value())
        assertEquals("Unexpected server error", response.body?.message)
    }

    @Test
    fun `maps malformed methods and database failures without leaking details`() {
        val malformed = handler.malformedRequest(mock<HttpMessageNotReadableException>(), request)
        val method = handler.methodNotAllowed(HttpRequestMethodNotSupportedException("TRACE"), request)
        val database = handler.databaseUnavailable(mock<DataAccessException>(), request)

        assertEquals(400, malformed.statusCode.value())
        assertEquals(405, method.statusCode.value())
        assertEquals(503, database.statusCode.value())
        assertEquals("Database is temporarily unavailable", database.body?.message)
    }
}
