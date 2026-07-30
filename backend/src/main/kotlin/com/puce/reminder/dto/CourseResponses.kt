package com.puce.reminder.dto

import java.time.Instant

data class CourseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val joinCode: String,
    val ownerUserId: String,
    val createdAt: Instant,
)
