package com.puce.reminder.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CognitoTokenValidatorTest {
    private val validator = CognitoTokenValidator("daily-reminder-client")

    @Test
    fun `accepts an access token for the configured client`() {
        val result = validator.validate(jwt("access", "daily-reminder-client"))

        assertFalse(result.hasErrors())
    }

    @Test
    fun `rejects an id token`() {
        val result = validator.validate(jwt("id", "daily-reminder-client"))

        assertTrue(result.hasErrors())
    }

    @Test
    fun `rejects a token for another client`() {
        val result = validator.validate(jwt("access", "another-client"))

        assertTrue(result.hasErrors())
    }

    private fun jwt(tokenUse: String, clientId: String): Jwt =
        Jwt(
            "token",
            Instant.now(),
            Instant.now().plusSeconds(300),
            mapOf("alg" to "none"),
            mapOf(
                "sub" to "user-id",
                "token_use" to tokenUse,
                "client_id" to clientId,
            ),
        )
}
