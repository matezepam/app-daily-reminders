package com.puce.reminder.dto

data class RegistrationResponse(
    val userId: String?,
    val confirmed: Boolean,
    val deliveryDestination: String?,
    val deliveryMedium: String?,
)

data class AuthenticationResponse(
    val accessToken: String,
    val idToken: String,
    val refreshToken: String?,
    val expiresIn: Int,
    val tokenType: String,
)

data class MessageResponse(
    val message: String,
)
