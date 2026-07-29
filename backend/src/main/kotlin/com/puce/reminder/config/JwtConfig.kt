package com.puce.reminder.config

import com.puce.reminder.security.CognitoAuthoritiesConverter
import com.puce.reminder.security.CognitoTokenValidator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter

@Configuration
class JwtConfig {
    @Bean
    fun jwtDecoder(properties: CognitoProperties): JwtDecoder {
        if (properties.region.isBlank() || properties.userPoolId.isBlank() || properties.clientId.isBlank()) {
            return JwtDecoder { throw BadJwtException("Amazon Cognito JWT validation is not configured") }
        }

        val issuer = "https://cognito-idp.${properties.region}.amazonaws.com/${properties.userPoolId}"
        val decoder = NimbusJwtDecoder.withJwkSetUri("$issuer/.well-known/jwks.json").build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(issuer),
                CognitoTokenValidator(properties.clientId),
            ),
        )
        return decoder
    }

    @Bean
    fun jwtAuthenticationConverter(
        authoritiesConverter: CognitoAuthoritiesConverter,
    ): JwtAuthenticationConverter =
        JwtAuthenticationConverter().apply {
            setPrincipalClaimName("username")
            setJwtGrantedAuthoritiesConverter(authoritiesConverter)
        }
}
