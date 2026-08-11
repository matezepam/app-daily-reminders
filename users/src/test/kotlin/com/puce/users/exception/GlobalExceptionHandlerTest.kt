package com.puce.users.exception

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.dao.DataIntegrityViolationException

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()
    private val request = MockHttpServletRequest("GET", "/users/missing")

    @Test
    fun `maps user domain exceptions to consistent http errors`() {
        val notFound = handler.notFound(UserNotFoundException("User not found"), request)
        val conflict = handler.conflict(UserConflictException("User already exists"), request)
        val databaseConflict = handler.conflict(DataIntegrityViolationException("private database detail"), request)

        assertEquals(404, notFound.statusCode.value())
        assertEquals("User not found", notFound.body?.message)
        assertEquals("/users/missing", notFound.body?.path)
        assertEquals(409, conflict.statusCode.value())
        assertEquals("User data conflicts with an existing profile", databaseConflict.body?.message)
    }

    @Test
    fun `maps authentication and identity provider failures`() {
        val unauthorized = handler.unauthorized(UserAuthenticationException("Invalid Cognito group"), request)
        val unavailable = handler.identityProvider(IdentityProviderException("Cognito unavailable"), request)

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals(503, unavailable.statusCode.value())
    }

    @Test
    fun `maps field validation errors`() {
        val exception = mock<MethodArgumentNotValidException>()
        val bindingResult = mock<BindingResult>()
        whenever(exception.bindingResult).thenReturn(bindingResult)
        whenever(bindingResult.fieldErrors).thenReturn(listOf(FieldError("request", "fullName", "must not be blank")))

        val response = handler.validation(exception, request)

        assertEquals(400, response.statusCode.value())
        assertEquals("must not be blank", response.body?.fieldErrors?.get("fullName"))
    }

    @Test
    fun `hides unexpected server error details`() {
        val response = handler.unexpected(IllegalStateException("database details"), request)

        assertEquals(500, response.statusCode.value())
        assertEquals("Unexpected server error", response.body?.message)
    }
}
