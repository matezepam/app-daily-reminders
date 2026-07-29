package com.puce.reminder.services

import com.puce.reminder.config.CognitoProperties
import com.puce.reminder.dto.AuthenticationResponse
import com.puce.reminder.dto.ConfirmRegistrationRequest
import com.puce.reminder.dto.LoginRequest
import com.puce.reminder.dto.RegisterRequest
import com.puce.reminder.dto.RegistrationResponse
import com.puce.reminder.exceptions.ApiException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkClientException
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType
import software.amazon.awssdk.services.cognitoidentityprovider.model.CodeMismatchException
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import software.amazon.awssdk.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ExpiredCodeException
import software.amazon.awssdk.services.cognitoidentityprovider.model.InitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidParameterException
import software.amazon.awssdk.services.cognitoidentityprovider.model.InvalidPasswordException
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.SignUpRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.TooManyRequestsException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class CognitoAuthService(
    private val cognitoClient: CognitoIdentityProviderClient,
    private val properties: CognitoProperties,
) {
    fun register(request: RegisterRequest): RegistrationResponse = execute {
        requireConfiguration()
        val username = request.email.trim().lowercase()
        val builder = SignUpRequest.builder()
            .clientId(properties.clientId)
            .username(username)
            .password(request.password)
            .userAttributes(AttributeType.builder().name("email").value(username).build())
        secretHash(username)?.let(builder::secretHash)

        val response = cognitoClient.signUp(builder.build())
        val delivery = response.codeDeliveryDetails()
        RegistrationResponse(
            userId = response.userSub(),
            confirmed = response.userConfirmed(),
            deliveryDestination = delivery?.destination(),
            deliveryMedium = delivery?.deliveryMediumAsString(),
        )
    }

    fun confirmRegistration(request: ConfirmRegistrationRequest) {
        execute {
            requireConfiguration()
            val username = request.email.trim().lowercase()
            val builder = ConfirmSignUpRequest.builder()
                .clientId(properties.clientId)
                .username(username)
                .confirmationCode(request.code)
            secretHash(username)?.let(builder::secretHash)
            cognitoClient.confirmSignUp(builder.build())
        }
    }

    fun login(request: LoginRequest): AuthenticationResponse = execute {
        requireConfiguration()
        val username = request.email.trim().lowercase()
        val parameters = mutableMapOf(
            "USERNAME" to username,
            "PASSWORD" to request.password,
        )
        secretHash(username)?.let { parameters["SECRET_HASH"] = it }

        val response = cognitoClient.initiateAuth(
            InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(properties.clientId)
                .authParameters(parameters)
                .build(),
        )
        val authentication = response.authenticationResult()
            ?: throw ApiException(HttpStatus.CONFLICT, "Authentication requires an additional challenge")

        AuthenticationResponse(
            accessToken = authentication.accessToken(),
            idToken = authentication.idToken(),
            refreshToken = authentication.refreshToken(),
            expiresIn = authentication.expiresIn(),
            tokenType = authentication.tokenType(),
        )
    }

    private fun requireConfiguration() {
        if (properties.clientId.isBlank()) {
            throw ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Amazon Cognito is not configured")
        }
    }

    private fun secretHash(username: String): String? {
        if (properties.clientSecret.isBlank()) {
            return null
        }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(properties.clientSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal("$username${properties.clientId}".toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    private fun <T> execute(operation: () -> T): T =
        try {
            operation()
        } catch (exception: ApiException) {
            throw exception
        } catch (exception: CognitoIdentityProviderException) {
            throw mapCognitoException(exception)
        } catch (exception: SdkClientException) {
            throw ApiException(HttpStatus.BAD_GATEWAY, "Amazon Cognito is unavailable")
        }

    private fun mapCognitoException(exception: CognitoIdentityProviderException): ApiException =
        when (exception) {
            is UsernameExistsException -> ApiException(HttpStatus.CONFLICT, "An account already exists for this email")
            is CodeMismatchException -> ApiException(HttpStatus.BAD_REQUEST, "The confirmation code is invalid")
            is ExpiredCodeException -> ApiException(HttpStatus.BAD_REQUEST, "The confirmation code has expired")
            is InvalidPasswordException -> ApiException(HttpStatus.BAD_REQUEST, "The password does not meet the security policy")
            is InvalidParameterException -> ApiException(HttpStatus.BAD_REQUEST, "The authentication request is invalid")
            is UserNotConfirmedException -> ApiException(HttpStatus.FORBIDDEN, "The account has not been confirmed")
            is NotAuthorizedException, is UserNotFoundException -> ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password")
            is TooManyRequestsException -> ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many authentication attempts")
            else -> ApiException(HttpStatus.BAD_GATEWAY, "Amazon Cognito rejected the request")
        }
}
