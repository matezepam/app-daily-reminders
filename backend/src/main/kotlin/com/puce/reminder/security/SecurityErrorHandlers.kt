package com.puce.reminder.security

import com.puce.reminder.exceptions.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Component
class ApiAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required")
    }

    private fun writeError(response: HttpServletResponse, status: HttpStatus, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            ApiErrorResponse(Instant.now(), status.value(), status.reasonPhrase, message),
        )
    }
}

@Component
class ApiAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: org.springframework.security.access.AccessDeniedException,
    ) {
        val status = HttpStatus.FORBIDDEN
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        objectMapper.writeValue(
            response.outputStream,
            ApiErrorResponse(Instant.now(), status.value(), status.reasonPhrase, "Insufficient permissions"),
        )
    }
}
