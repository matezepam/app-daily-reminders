package com.puce.reminder.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CognitoAuthoritiesConverterTest {
    private val converter = CognitoAuthoritiesConverter()

    @Test
    fun `maps known Cognito groups to role authorities`() {
        val jwt = jwtWithGroups("student", "TEACHER", "unknown", "STUDENT")

        val authorities = converter.convert(jwt).map { authority -> authority.authority }

        assertEquals(listOf("ROLE_STUDENT", "ROLE_TEACHER"), authorities)
    }

    @Test
    fun `returns no role when groups claim is absent`() {
        val jwt = Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            mapOf("alg" to "none"),
            mapOf("sub" to "user-id"),
        )

        assertEquals(emptyList<String>(), converter.convert(jwt))
    }

    private fun jwtWithGroups(vararg groups: String): Jwt =
        Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            mapOf("alg" to "none"),
            mapOf("sub" to "user-id", "cognito:groups" to groups.toList()),
        )
}
