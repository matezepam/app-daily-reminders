package com.puce.reminder.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CognitoGroupsConverterTest {
    private val converter = SecurityConfig().jwtAuthenticationConverter()

    @Test
    fun `maps the exact student group`() {
        assertEquals(setOf("ROLE_STUDENT"), authorities(token(listOf("STUDENT"))))
    }

    @Test
    fun `maps the exact administrator group`() {
        assertEquals(setOf("ROLE_ADMIN"), authorities(token(listOf("ADMIN"))))
    }

    @Test
    fun `rejects missing legacy unknown and ambiguous groups`() {
        assertEquals(emptySet<String>(), authorities(token(emptyList())))
        assertEquals(emptySet<String>(), authorities(token(listOf("USER"))))
        assertEquals(emptySet<String>(), authorities(token(listOf("PROFESSOR"))))
        assertEquals(emptySet<String>(), authorities(token(listOf("GUEST"))))
        assertEquals(emptySet<String>(), authorities(token(listOf("ADMIN", "STUDENT"))))
    }

    private fun authorities(jwt: Jwt) = requireNotNull(converter.convert(jwt)).authorities
        .map { it.authority }
        .filterNotNull()
        .filter { it.startsWith("ROLE_") }
        .toSet()

    private fun token(groups: List<String>): Jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("cognito:groups", groups)
        .build()
}
