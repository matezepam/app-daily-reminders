package com.puce.users.client

import com.puce.users.config.CognitoProperties
import com.puce.users.exception.IdentityProviderException
import com.puce.users.exception.UserAuthenticationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestClient
import org.springframework.web.client.ResourceAccessException
import org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import tools.jackson.databind.json.JsonMapper

class CognitoProfileClientTest {
    private val properties = CognitoProperties(
        region = "us-east-1",
        userPoolId = "us-east-1_example",
        clientId = "client-id",
        domain = "https://example.auth.us-east-1.amazoncognito.com/",
    )

    @Test
    fun `loads OAuth profile from user info endpoint`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(once(), requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer access-token"))
            .andRespond(withSuccess("""{"email":"Student@Example.com","name":"Student Name"}""", MediaType.APPLICATION_JSON))

        val profile = client(builder).profile(jwt(scope = "openid email profile"))

        assertEquals(CognitoProfile("student@example.com", "Student Name"), profile)
        server.verify()
    }

    @Test
    fun `loads direct authentication profile from get user endpoint`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(once(), requestTo(properties.identityProviderEndpoint))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("X-Amz-Target", "AWSCognitoIdentityProviderService.GetUser"))
            .andRespond(withSuccess(
                """{"UserAttributes":[{"Name":"email","Value":"direct@example.com"},{"Name":"name","Value":"Direct User"}]}""",
                MediaType.valueOf("application/x-amz-json-1.1"),
            ))

        val profile = client(builder).profile(jwt())

        assertEquals(CognitoProfile("direct@example.com", "Direct User"), profile)
        server.verify()
    }

    @Test
    fun `uses normalized email prefix when name is absent`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond(withSuccess("""{"email":"Student.Name@Example.com"}""", MediaType.APPLICATION_JSON))

        val profile = client(builder).profile(jwt(scope = "openid"))

        assertEquals("student.name", profile.fullName)
    }

    @Test
    fun `rejects an invalid access token`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond(withUnauthorizedRequest())

        assertThrows(UserAuthenticationException::class.java) {
            client(builder).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `rejects profile without email`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond(withSuccess("""{"name":"No Email"}""", MediaType.APPLICATION_JSON))

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `requires Cognito domain for OAuth token`() {
        val noDomain = properties.copy(domain = "")
        assertThrows(IdentityProviderException::class.java) {
            client(RestClient.builder(), noDomain).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `maps Cognito server error to identity provider error`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond(withServerError())

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `maps network error to identity provider error`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond { throw ResourceAccessException("offline") }

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `rejects empty user info response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo("${properties.normalizedDomain}/oauth2/userInfo"))
            .andRespond(withNoContent())

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt(scope = "openid"))
        }
    }

    @Test
    fun `rejects empty get user response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(properties.identityProviderEndpoint)).andRespond(withNoContent())

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt(scope = "   "))
        }
    }

    @Test
    fun `rejects unreadable Cognito response`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(requestTo(properties.identityProviderEndpoint))
            .andRespond(withSuccess("not-json", MediaType.valueOf("application/x-amz-json-1.1")))

        assertThrows(IdentityProviderException::class.java) {
            client(builder).profile(jwt())
        }
    }

    private fun client(
        builder: RestClient.Builder,
        cognitoProperties: CognitoProperties = properties,
    ) = CognitoProfileClient(builder, cognitoProperties, JsonMapper.builder().build())

    private fun jwt(scope: String? = null): Jwt {
        val builder = Jwt.withTokenValue("access-token").header("alg", "none").subject("subject")
        if (scope != null) builder.claim("scope", scope)
        return builder.build()
    }
}
