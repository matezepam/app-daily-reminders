package com.puce.users.security

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class CognitoJwtValidatorsTest {
    @Test
    fun `accepts access token for configured client`() {
        val jwt = token("access", "client-a")
        assertFalse(CognitoTokenUseValidator().validate(jwt).hasErrors())
        assertFalse(CognitoClientIdValidator("client-a").validate(jwt).hasErrors())
    }

    @Test
    fun `rejects id token and wrong client`() {
        val jwt = token("id", "client-b")
        assertTrue(CognitoTokenUseValidator().validate(jwt).hasErrors())
        assertTrue(CognitoClientIdValidator("client-a").validate(jwt).hasErrors())
    }

    private fun token(tokenUse: String, clientId: String): Jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("subject")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("token_use", tokenUse)
        .claim("client_id", clientId)
        .build()
}
