package com.puce.reminder.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.cognito.region=us-east-1",
        "app.cognito.user-pool-id=us-east-1_test",
        "app.cognito.client-id=daily-reminder-client",
    ],
)
@AutoConfigureMockMvc
class SecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `private endpoints require authentication`() {
        mockMvc.perform(get("/api/v1/session"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Authentication is required"))
    }

    @Test
    fun `authentication endpoints remain public`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `session exposes the authenticated identity and roles`() {
        mockMvc.perform(
            get("/api/v1/session")
                .with(
                    jwt()
                        .jwt { token -> token.subject("user-id").claim("username", "mateo") }
                        .authorities(SimpleGrantedAuthority("ROLE_TEACHER")),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value("user-id"))
            .andExpect(jsonPath("$.username").value("mateo"))
            .andExpect(jsonPath("$.roles[0]").value("TEACHER"))
    }

    @Test
    fun `student role cannot access teacher routes`() {
        mockMvc.perform(
            get("/api/v1/teacher/protected")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_STUDENT"))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Insufficient permissions"))
    }

    @Test
    fun `teacher role cannot access admin routes`() {
        mockMvc.perform(
            get("/api/v1/admin/protected")
                .with(jwt().authorities(SimpleGrantedAuthority("ROLE_TEACHER"))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("Insufficient permissions"))
    }
}
