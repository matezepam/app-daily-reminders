package com.puce.reminder.controller

import com.puce.reminder.config.CognitoConfig
import com.puce.reminder.config.SecurityConfig
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.service.CourseService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(CourseController::class)
@Import(SecurityConfig::class, CognitoConfig::class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class)
@TestPropertySource(properties = [
    "app.security.cors-allowed-origins=http://localhost:9090",
    "app.cognito.region=us-east-1",
    "app.cognito.user-pool-id=us-east-1_test",
    "app.cognito.client-id=test-client",
])
class CourseSecurityTest(@Autowired private val mvc: MockMvc) {
    @MockitoBean
    private lateinit var service: CourseService

    @Test
    fun `course endpoint without token returns 401`() {
        mvc.perform(get("/academic-reminder/courses/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `user cannot create course`() {
        mvc.perform(
            post("/academic-reminder/courses")
                .with(jwt().jwt { it.subject("student-1") }.authorities(SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Architecture\"}"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `professor creates course`() {
        whenever(service.create(any<CourseCreateRequest>())).thenReturn(CourseResponse(1, "Architecture", null, "ARC-12345", "professor-1", 0, Instant.now(), true))
        mvc.perform(
            post("/academic-reminder/courses")
                .with(jwt().jwt { it.subject("professor-1") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Architecture\"}"),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `student joins course`() {
        whenever(service.join("ARC-12345")).thenReturn(CourseResponse(1, "Architecture", null, "ARC-12345", "professor-1", 1, Instant.now(), false))
        mvc.perform(
            post("/academic-reminder/courses/join")
                .with(jwt().jwt { it.subject("student-1") }.authorities(SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ARC-12345\"}"),
        ).andExpect(status().isOk)
    }

    @Test
    fun `professor cannot join course as student`() {
        mvc.perform(
            post("/academic-reminder/courses/join")
                .with(jwt().jwt { it.subject("professor-1") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"ARC-12345\"}"),
        ).andExpect(status().isForbidden)
    }
}
