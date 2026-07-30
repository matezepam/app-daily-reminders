package com.puce.reminder.controllers

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:courses;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.cognito.region=us-east-1",
        "app.cognito.user-pool-id=us-east-1_test",
        "app.cognito.client-id=daily-reminder-client",
    ],
)
@AutoConfigureMockMvc
@Transactional
class CourseControllerIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `teacher creates a course with a unique join code`() {
        mockMvc.perform(
            post("/api/v1/courses")
                .with(
                    jwt()
                        .jwt { token -> token.subject("teacher-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_TEACHER")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Matematicas II","description":"Periodo actual"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Matematicas II"))
            .andExpect(jsonPath("$.ownerUserId").value("teacher-id"))
            .andExpect(jsonPath("$.joinCode").value(org.hamcrest.Matchers.matchesPattern("MAT-[A-Z2-9]{5}")))
    }

    @Test
    fun `student cannot create a course`() {
        mockMvc.perform(
            post("/api/v1/courses")
                .with(
                    jwt()
                        .jwt { token -> token.subject("student-id") }
                        .authorities(SimpleGrantedAuthority("ROLE_STUDENT")),
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Curso no permitido"}"""),
        ).andExpect(status().isForbidden)
    }
}
