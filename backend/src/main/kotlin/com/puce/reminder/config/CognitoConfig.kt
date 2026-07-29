package com.puce.reminder.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient

@ConfigurationProperties(prefix = "app.cognito")
data class CognitoProperties(
    var region: String = "us-east-1",
    var userPoolId: String = "",
    var clientId: String = "",
    var clientSecret: String = "",
)

@Configuration
@EnableConfigurationProperties(CognitoProperties::class)
class CognitoConfig {
    @Bean
    fun cognitoIdentityProviderClient(properties: CognitoProperties): CognitoIdentityProviderClient =
        CognitoIdentityProviderClient.builder()
            .region(Region.of(properties.region))
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .build()
}
