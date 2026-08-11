package com.puce.users.client

import com.puce.users.config.CognitoProperties
import com.puce.users.exception.IdentityProviderException
import com.puce.users.exception.UserAuthenticationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class CognitoProfileClient(
    builder: RestClient.Builder,
    private val properties: CognitoProperties,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.build()

    fun profile(jwt: Jwt): CognitoProfile {
        log.info("event=cognito.profile.request | msg=Retrieving Cognito user attributes")
        return try {
            val attributes = if (jwt.scopes().contains("openid")) fromUserInfo(jwt.tokenValue) else fromGetUser(jwt.tokenValue)
            val email = attributes["email"]?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
                ?: throw IdentityProviderException("Cognito did not return a readable email attribute")
            val name = attributes["name"]?.trim()?.takeIf(String::isNotEmpty) ?: email.substringBefore('@')
            log.info("event=cognito.profile.received | msg=Cognito user attributes retrieved")
            CognitoProfile(email, name)
        } catch (exception: RestClientResponseException) {
            if (exception.statusCode.value() == 401 || exception.statusCode.value() == 403) {
                throw UserAuthenticationException("Cognito rejected the access token")
            }
            log.warn("event=cognito.profile.failed | msg=Cognito rejected the profile request | status={}", exception.statusCode.value())
            throw IdentityProviderException("Cognito user profile is unavailable")
        } catch (exception: ResourceAccessException) {
            log.warn("event=cognito.profile.failed | msg=Cognito profile request failed | reason={}", exception.javaClass.simpleName)
            throw IdentityProviderException("Cognito user profile is unavailable")
        }
    }

    private fun fromUserInfo(token: String): Map<String, String> {
        if (properties.normalizedDomain.isBlank()) throw IdentityProviderException("COGNITO_DOMAIN is required for OAuth access tokens")
        val body = client.get()
            .uri("${properties.normalizedDomain}/oauth2/userInfo")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(String::class.java)
        val profile = parse(body)
        return mapOf(
            "email" to profile.text("email"),
            "name" to profile.text("name"),
        )
    }

    private fun fromGetUser(token: String): Map<String, String> {
        val body = client.post()
            .uri(properties.identityProviderEndpoint)
            .header("X-Amz-Target", "AWSCognitoIdentityProviderService.GetUser")
            .contentType(MediaType.valueOf("application/x-amz-json-1.1"))
            .body("""{"AccessToken":"$token"}""")
            .retrieve()
            .body(String::class.java)
        return parse(body).path("UserAttributes")
            .associate { attribute -> attribute.text("Name") to attribute.text("Value") }
    }

    private fun parse(body: String?): JsonNode {
        if (body.isNullOrBlank()) throw IdentityProviderException("Cognito returned an empty user profile")
        return try {
            objectMapper.readTree(body)
        } catch (_: JacksonException) {
            throw IdentityProviderException("Cognito returned an unreadable user profile")
        }
    }

    private fun Jwt.scopes(): Set<String> = getClaimAsString("scope")
        ?.split(Regex("\\s+"))
        ?.filter(String::isNotBlank)
        ?.toSet()
        ?: emptySet()

    private fun JsonNode.text(field: String): String = path(field).asString("")
}

data class CognitoProfile(val email: String, val fullName: String)
