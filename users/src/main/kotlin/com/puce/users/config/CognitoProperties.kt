package com.puce.users.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.cognito")
data class CognitoProperties(
    val region: String,
    val userPoolId: String,
    val clientId: String,
    val domain: String = "",
    val issuerUri: String? = null,
) {
    val issuer: String
        get() = issuerUri?.trimEnd('/')?.takeIf(String::isNotBlank)
            ?: "https://cognito-idp.$region.amazonaws.com/$userPoolId"

    val jwkSetUri: String
        get() = "$issuer/.well-known/jwks.json"

    val normalizedDomain: String
        get() = domain.trim().trimEnd('/')

    val identityProviderEndpoint: String
        get() = "https://cognito-idp.$region.amazonaws.com/"
}
