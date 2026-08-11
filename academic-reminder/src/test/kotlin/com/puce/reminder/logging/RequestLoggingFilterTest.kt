package com.puce.reminder.logging

import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class RequestLoggingFilterTest {
    @AfterEach
    fun cleanSecurityContext() {
        SecurityContextHolder.clearContext()
        MDC.clear()
    }

    @Test
    fun `adds authenticated subject to request context and clears it after response`() {
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("professor-1")
            .build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
        val request = MockHttpServletRequest("POST", "/academic-reminder/courses")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, servletResponse ->
            assertEquals("professor-1", MDC.get("sub"))
            (servletResponse as HttpServletResponse).status = 201
        }

        RequestLoggingFilter().doFilter(request, response, chain)

        assertEquals(201, response.status)
        assertNull(MDC.get("sub"))
    }
}
