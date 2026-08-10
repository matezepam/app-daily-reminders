package com.puce.reminder.controller

import com.puce.reminder.config.CognitoConfig
import com.puce.reminder.config.SecurityConfig
import com.puce.reminder.dto.ReminderRequest
import com.puce.reminder.dto.ReminderResponse
import com.puce.reminder.entity.ReminderPriority
import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.entity.ReminderType
import com.puce.reminder.service.ReminderService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@WebMvcTest(ReminderController::class)
@Import(SecurityConfig::class, CognitoConfig::class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class)
@TestPropertySource(properties = [
    "app.security.cors-allowed-origins=http://localhost:9090",
    "app.cognito.region=us-east-1",
    "app.cognito.user-pool-id=us-east-1_test",
    "app.cognito.client-id=test-client",
])
class ReminderSecurityTest(@Autowired private val mvc: MockMvc) {
    @MockitoBean
    private lateinit var service: ReminderService

    private val payload = """{"title":"Review","type":"TASK","dueAt":"2099-08-10T12:00:00Z","priority":"MEDIUM","notificationOffsetsMinutes":[10]}"""

    @Test
    fun `student creates personal reminder`() {
        whenever(service.createPersonal(any<ReminderRequest>())).thenReturn(response())
        mvc.perform(
            post("/academic-reminder/reminders")
                .with(role("student-1", "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `professor creates personal reminder`() {
        whenever(service.createPersonal(any<ReminderRequest>())).thenReturn(response())
        mvc.perform(
            post("/academic-reminder/reminders")
                .with(role("professor-1", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `professor creates reminder for owned course`() {
        whenever(service.createForCourse(any(), any<ReminderRequest>())).thenReturn(response(courseId = 3))
        mvc.perform(
            post("/academic-reminder/courses/3/reminders")
                .with(role("professor-1", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isCreated)
    }

    @Test
    fun `student cannot publish reminder for course`() {
        mvc.perform(
            post("/academic-reminder/courses/3/reminders")
                .with(role("student-1", "ROLE_STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `professor can complete personal reminder`() {
        whenever(service.complete(8)).thenReturn(response(status = ReminderStatus.COMPLETED))
        mvc.perform(
            patch("/academic-reminder/reminders/8/complete")
                .with(role("professor-1", "ROLE_ADMIN")),
        ).andExpect(status().isOk)
    }

    private fun role(subject: String, authority: String) = jwt()
        .jwt { it.subject(subject) }
        .authorities(SimpleGrantedAuthority(authority))

    private fun response(
        courseId: Long? = null,
        status: ReminderStatus = ReminderStatus.PENDING,
    ) = ReminderResponse(
        id = 8,
        courseId = courseId,
        courseName = courseId?.let { "Architecture" },
        personal = courseId == null,
        title = "Review",
        description = null,
        type = ReminderType.TASK,
        dueAt = Instant.parse("2099-08-10T12:00:00Z"),
        priority = ReminderPriority.MEDIUM,
        customPriority = null,
        status = status,
        editable = true,
        createdAt = Instant.now(),
    )
}
