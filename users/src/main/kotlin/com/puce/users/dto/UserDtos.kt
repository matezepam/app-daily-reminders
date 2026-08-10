package com.puce.users.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class UpdateUserRequest(
    @field:NotBlank @field:Size(max = 120) val fullName: String,
)

data class UserResponse(
    val id: Long,
    val cognitoSub: String,
    val email: String,
    val fullName: String,
    val role: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val fieldErrors: Map<String, String>? = null,
)
