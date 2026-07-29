package com.puce.reminder.services

import com.puce.reminder.config.CognitoProperties
import com.puce.reminder.dto.LoginRequest
import com.puce.reminder.dto.RegisterRequest
import com.puce.reminder.exceptions.ApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import software.amazon.awssdk.services.cognitoidentityprovider.model.DeliveryMediumType
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpResponse
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException

class CognitoAuthServiceTest {
    private lateinit var cognitoClient: CognitoIdentityProviderClient
    private lateinit var authService: CognitoAuthService

    @BeforeEach
    fun setUp() {
        cognitoClient = mock(CognitoIdentityProviderClient::class.java)
        authService = CognitoAuthService(
            cognitoClient,
            CognitoProperties(clientId = "client-id"),
        )
    }

    @Test
    fun `register sends normalized email to Cognito`() {
        `when`(cognitoClient.signUp(any(SignUpRequest::class.java))).thenReturn(
            SignUpResponse.builder()
                .userSub("user-id")
                .userConfirmed(false)
                .codeDeliveryDetails(
                    CodeDeliveryDetailsType.builder()
                        .destination("m***@example.com")
                        .deliveryMedium(DeliveryMediumType.EMAIL)
                        .build(),
                )
                .build(),
        )

        val response = authService.register(RegisterRequest(" Mateo@Example.com ", "Password1!"))

        val captor = ArgumentCaptor.forClass(SignUpRequest::class.java)
        verify(cognitoClient).signUp(captor.capture())
        assertEquals("mateo@example.com", captor.value.username())
        assertEquals("user-id", response.userId)
        assertEquals("EMAIL", response.deliveryMedium)
    }

    @Test
    fun `login returns tokens issued by Cognito`() {
        `when`(cognitoClient.initiateAuth(any(InitiateAuthRequest::class.java))).thenReturn(
            InitiateAuthResponse.builder()
                .authenticationResult(
                    AuthenticationResultType.builder()
                        .accessToken("access-token")
                        .idToken("id-token")
                        .refreshToken("refresh-token")
                        .expiresIn(3600)
                        .tokenType("Bearer")
                        .build(),
                )
                .build(),
        )

        val response = authService.login(LoginRequest("mateo@example.com", "Password1!"))

        assertEquals("access-token", response.accessToken)
        assertEquals("id-token", response.idToken)
        assertEquals("refresh-token", response.refreshToken)
        assertEquals(3600, response.expiresIn)
    }

    @Test
    fun `register maps duplicate accounts to conflict`() {
        `when`(cognitoClient.signUp(any(SignUpRequest::class.java))).thenThrow(
            UsernameExistsException.builder().message("duplicate").build(),
        )

        val exception = assertThrows(ApiException::class.java) {
            authService.register(RegisterRequest("mateo@example.com", "Password1!"))
        }

        assertEquals(HttpStatus.CONFLICT, exception.status)
        assertNotNull(exception.message)
    }

    @Test
    fun `register includes secret hash for confidential clients`() {
        authService = CognitoAuthService(
            cognitoClient,
            CognitoProperties(clientId = "client-id", clientSecret = "secret"),
        )
        `when`(cognitoClient.signUp(any(SignUpRequest::class.java))).thenReturn(
            SignUpResponse.builder().userSub("user-id").userConfirmed(false).build(),
        )

        authService.register(RegisterRequest("mateo@example.com", "Password1!"))

        val captor = ArgumentCaptor.forClass(SignUpRequest::class.java)
        verify(cognitoClient).signUp(captor.capture())
        assertEquals("1rSHJ9beIu3bhwGuzsghsEcWcdDY8xWJTgJv2OVHSGU=", captor.value.secretHash())
    }
}
