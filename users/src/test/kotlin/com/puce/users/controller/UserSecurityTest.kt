package com.puce.users.controller

import com.puce.users.config.CognitoProperties
import com.puce.users.config.SecurityConfig
import com.puce.users.dto.UserResponse
import com.puce.users.dto.UpdateUserRequest
import com.puce.users.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.http.MediaType
import java.time.Instant

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class)
@EnableConfigurationProperties(CognitoProperties::class)
@TestPropertySource(properties = [
    "app.cognito.region=us-east-1",
    "app.cognito.user-pool-id=us-east-1_test",
    "app.cognito.client-id=test-client",
])
class UserSecurityTest(@Autowired private val mvc: MockMvc) {
    @MockitoBean
    private lateinit var service: UserService

    @Test
    fun `me without token returns 401`() {
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `valid jwt can synchronize own profile`() {
        whenever(service.synchronize(org.mockito.kotlin.any())).thenReturn(response("student-1"))
        mvc.perform(get("/users/me").with(jwt().jwt { it.subject("student-1") }.authorities(SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk)
    }

    @Test
    fun `valid jwt can update own profile`() {
        whenever(service.update(any(), any<UpdateUserRequest>())).thenReturn(response("student-1"))
        mvc.perform(
            put("/users/me")
                .with(jwt().jwt { it.subject("student-1") }.authorities(SimpleGrantedAuthority("ROLE_STUDENT")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fullName":"Updated Student"}"""),
        ).andExpect(status().isOk)
    }

    @Test
    fun `user can look up a profile for an authorized cross service request`() {
        whenever(service.findBySubject("other")).thenReturn(response("other"))
        mvc.perform(get("/users/other").with(jwt().jwt { it.subject("student-1") }.authorities(SimpleGrantedAuthority("ROLE_STUDENT"))))
            .andExpect(status().isOk)
    }

    @Test
    fun `unrecognized role cannot look up a profile`() {
        mvc.perform(get("/users/other").with(jwt().jwt { it.subject("guest-1") }.authorities(SimpleGrantedAuthority("ROLE_GUEST"))))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `administrator can look up student profile`() {
        whenever(service.findBySubject("student-1")).thenReturn(response("student-1"))
        mvc.perform(get("/users/student-1").with(jwt().jwt { it.subject("professor-1") }.authorities(SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isOk)
    }

    private fun response(subject: String) = UserResponse(1, subject, "$subject@example.com", "Test User", "STUDENT", Instant.now(), Instant.now())
}
