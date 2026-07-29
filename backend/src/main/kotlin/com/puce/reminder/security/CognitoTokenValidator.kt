package com.puce.reminder.security

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

class CognitoTokenValidator(
    private val clientId: String,
) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        if (token.getClaimAsString("token_use") != "access") {
            return failure("The token is not a Cognito access token")
        }
        if (token.getClaimAsString("client_id") != clientId) {
            return failure("The token was issued for another application client")
        }
        return OAuth2TokenValidatorResult.success()
    }

    private fun failure(description: String): OAuth2TokenValidatorResult =
        OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", description, null))
}
