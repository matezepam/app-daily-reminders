package com.puce.reminder.dto

import com.puce.reminder.entity.AttendanceStatus
import com.puce.reminder.entity.ReminderPriority
import com.puce.reminder.entity.ReminderType
import jakarta.validation.constraints.*
import java.time.Instant
import java.time.LocalDate

data class CourseCreateRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 255) val description: String? = null,
)

data class JoinCourseRequest(
    @field:NotBlank @field:Size(max = 20) val code: String,
)

data class ReminderRequest(
    @field:NotBlank @field:Size(max = 100) val title: String,
    @field:Size(max = 500) val description: String? = null,
    val type: ReminderType,
    @field:Future val dueAt: Instant,
    val priority: ReminderPriority = ReminderPriority.MEDIUM,
    @field:Positive val priorityCategoryId: Long? = null,
    @field:Size(min = 1, max = 10)
    val notificationOffsetsMinutes: Set<@Positive Long> = setOf(60),
)

data class NotificationRequest(
    @field:Size(min = 1, max = 10)
    val offsetsMinutes: Set<@Positive Long>,
)

data class PriorityCategoryRequest(
    @field:NotBlank @field:Size(max = 40) val name: String,
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$") val color: String,
    @field:Min(0) @field:Max(100) val sortOrder: Int = 0,
)

data class ReminderPriorityOverrideRequest(@field:Positive val priorityCategoryId: Long?)

data class ActivityRequest(
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:Size(max = 1000) val description: String? = null,
    @field:Future val dueAt: Instant,
)

data class AttendanceRequest(
    @field:NotBlank @field:Size(max = 100) val studentUserId: String,
    @field:PastOrPresent val attendanceDate: LocalDate,
    val status: AttendanceStatus,
)
