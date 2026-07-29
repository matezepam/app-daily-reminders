package com.puce.reminder.dto

data class SessionResponse(
    val userId: String,
    val username: String,
    val roles: Set<String>,
)
