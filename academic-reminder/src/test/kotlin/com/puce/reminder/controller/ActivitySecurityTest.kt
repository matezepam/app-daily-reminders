package com.puce.reminder.controller

import com.puce.reminder.config.CognitoConfig
import com.puce.reminder.config.SecurityConfig
import com.puce.reminder.dto.ActivityRequest
import com.puce.reminder.dto.ActivityResponse
import com.puce.reminder.service.ActivityService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(ActivityController::class)
@Import(SecurityConfig::class, CognitoConfig::class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class)
@TestPropertySource(properties = [
    "app.security.cors-allowed-origins=http://localhost:9090",
    "app.cognito.region=us-east-1",
    "app.cognito.user-pool-id=us-east-1_test",
    "app.cognito.client-id=test-client",
])
class ActivitySecurityTest(@Autowired private val mvc: MockMvc) {
    @MockitoBean
    private lateinit var service: ActivityService

    private val payload = """{"title":"Defense","dueAt":"2099-08-10T12:00:00Z"}"""

    @Test
    fun `professor creates course activity`() {
        whenever(service.create(any(), any<ActivityRequest>())).thenReturn(response())
        mvc.perform(
            post("/academic-reminder/courses/1/activities")
                .with(role("professor-1", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `student cannot create course activity`() {
        mvc.perform(
            post("/academic-reminder/courses/1/activities")
                .with(role("student-1", "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `student completes and reopens activity`() {
        whenever(service.complete(2)).thenReturn(response(completed = true))
        whenever(service.uncomplete(2)).thenReturn(response())
        mvc.perform(
            patch("/academic-reminder/activities/2/complete")
                .with(role("student-1", "ROLE_STUDENT")),
        ).andExpect(status().isOk)
        mvc.perform(
            delete("/academic-reminder/activities/2/completion")
                .with(role("student-1", "ROLE_STUDENT")),
        ).andExpect(status().isOk)
    }

    @Test
    fun `professor cannot impersonate student completion`() {
        mvc.perform(
            patch("/academic-reminder/activities/2/complete")
                .with(role("professor-1", "ROLE_ADMIN")),
        ).andExpect(status().isForbidden)
        mvc.perform(
            delete("/academic-reminder/activities/2/completion")
                .with(role("professor-1", "ROLE_ADMIN")),
        ).andExpect(status().isForbidden)
    }

    private fun role(subject: String, authority: String) = jwt()
        .jwt { it.subject(subject) }
        .authorities(SimpleGrantedAuthority(authority))

    private fun response(completed: Boolean = false) = ActivityResponse(
        id = 2,
        courseId = 1,
        title = "Defense",
        description = null,
        dueAt = Instant.parse("2099-08-10T12:00:00Z"),
        createdByUserId = "professor-1",
        completed = completed,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )
}
