package com.puce.reminder.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "app.cognito")
data class CognitoProperties(
    var region: String = "us-east-1",
    var userPoolId: String = "",
    var clientId: String = "",
    var issuerUri: String = "",
) {
    val issuer: String get() = issuerUri.trimEnd('/').takeIf(String::isNotBlank)
        ?: "https://cognito-idp.$region.amazonaws.com/$userPoolId"
    val jwkSetUri: String get() = "$issuer/.well-known/jwks.json"
}

@Configuration
@EnableConfigurationProperties(CognitoProperties::class)
class CognitoConfig
