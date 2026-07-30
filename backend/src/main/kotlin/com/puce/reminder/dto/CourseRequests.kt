package com.puce.reminder.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CourseCreateRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:Size(max = 255)
    val description: String? = null,
)

data class JoinCourseRequest(
    @field:NotBlank
    @field:Size(max = 20)
    val code: String,
)
