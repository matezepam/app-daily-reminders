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
