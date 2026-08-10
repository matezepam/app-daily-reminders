package com.puce.reminder.config

import com.puce.reminder.dto.ApiError
import com.puce.reminder.security.CognitoClientIdValidator
import com.puce.reminder.security.CognitoTokenUseValidator
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.http.MediaType
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        converter: JwtAuthenticationConverter,
        objectMapper: ObjectMapper,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .cors { }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/actuator/health", "/error").permitAll()
                .anyRequest().hasAnyRole("STUDENT", "ADMIN")
        }
        .oauth2ResourceServer {
            it.jwt { jwt -> jwt.jwtAuthenticationConverter(converter) }
            it.authenticationEntryPoint { request, response, _ ->
                writeError(request, response, objectMapper, 401, "Unauthorized", "Missing, invalid, or expired access token")
            }
            it.accessDeniedHandler { request, response, _ ->
                writeError(request, response, objectMapper, 403, "Forbidden", "You do not have permission to perform this operation")
            }
        }
        .build()

    @Bean
    fun corsConfigurationSource(
        @Value("\${app.security.cors-allowed-origins}") allowedOrigins: String,
    ): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            this.allowedOrigins = allowedOrigins.split(',').map(String::trim).filter(String::isNotEmpty)
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
            allowCredentials = true
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", configuration) }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter = JwtAuthenticationConverter().apply {
        setJwtGrantedAuthoritiesConverter(CognitoGroupsConverter())
    }

    @Bean
    fun jwtDecoder(properties: CognitoProperties): JwtDecoder {
        require(properties.userPoolId.isNotBlank()) { "COGNITO_USER_POOL_ID is required" }
        require(properties.clientId.isNotBlank()) { "COGNITO_CLIENT_ID is required" }
        val decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri).build()
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(properties.issuer), CognitoTokenUseValidator(), CognitoClientIdValidator(properties.clientId)))
        return decoder
    }

    private fun writeError(
        request: HttpServletRequest,
        response: HttpServletResponse,
        objectMapper: ObjectMapper,
        status: Int,
        error: String,
        message: String,
    ) {
        val path = request.requestURI.replace(Regex("[\\r\\n]"), "")
        val event = if (status == 401) "authentication.failed" else "authorization.denied"
        val previousSubject = MDC.get("sub")
        val authenticatedSubject = (SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken)?.token?.subject
        MDC.put("sub", authenticatedSubject ?: previousSubject ?: "anonimo")
        try {
            log.info("event=http.request | msg={} {}", request.method, path)
            log.warn("event={} | msg=Security request rejected | status={} path={}", event, status, path)
            response.status = status
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            objectMapper.writeValue(response.outputStream, ApiError(status = status, error = error, message = message, path = path))
        } finally {
            log.info("event=http.response | msg={} {} {}", response.status, request.method, path)
            if (previousSubject == null) MDC.remove("sub") else MDC.put("sub", previousSubject)
        }
    }
}

private class CognitoGroupsConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(source: Jwt): Collection<GrantedAuthority> {
        val groups = (source.getClaimAsStringList("cognito:groups") ?: emptyList())
            .map(String::uppercase)
            .filter { it == "ADMIN" || it == "STUDENT" }
            .distinct()
        return if (groups.size == 1) {
            listOf(SimpleGrantedAuthority("ROLE_${groups.single()}"))
        } else {
            emptyList()
        }
    }
}
