package com.puce.reminder.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Size(min = 8, max = 256)
    val password: String,
)

data class ConfirmRegistrationRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    @field:Pattern(regexp = "[0-9]{6}")
    val code: String,
)

data class LoginRequest(
    @field:NotBlank
    @field:Email
    val email: String,
    @field:NotBlank
    val password: String,
)
