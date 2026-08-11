package com.puce.reminder.dto

import com.puce.reminder.entity.AttendanceStatus
import com.puce.reminder.entity.ReminderPriority
import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.entity.ReminderType
import java.time.Instant

data class ExternalUserResponse(
    val id: Long,
    val cognitoSub: String,
    val email: String,
    val fullName: String,
    val role: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CourseResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val joinCode: String,
    val professorUserId: String,
    val memberCount: Long,
    val createdAt: Instant,
    val ownedByMe: Boolean,
)

data class PriorityCategoryResponse(
    val id: Long,
    val name: String,
    val color: String,
    val sortOrder: Int,
)

data class NotificationResponse(
    val id: Long,
    val reminderId: Long,
    val notifyAt: Instant,
    val sent: Boolean,
    val cancelled: Boolean,
)

data class ReminderResponse(
    val id: Long,
    val courseId: Long?,
    val courseName: String?,
    val personal: Boolean,
    val title: String,
    val description: String?,
    val type: ReminderType,
    val dueAt: Instant,
    val priority: ReminderPriority,
    val customPriority: PriorityCategoryResponse?,
    val status: ReminderStatus,
    val editable: Boolean,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val notifications: List<NotificationResponse> = emptyList(),
)

data class DashboardResponse(
    val pendingToday: Int,
    val upcoming: Int,
    val completedThisMonth: Int,
    val reminders: List<ReminderResponse>,
)

data class ActivityResponse(
    val id: Long,
    val courseId: Long,
    val title: String,
    val description: String?,
    val dueAt: Instant,
    val createdByUserId: String,
    val completed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val activityNumber: Long = id,
    val completedAt: Instant? = null,
    val completionCount: Int = 0,
    val completions: List<ActivityCompletionResponse> = emptyList(),
)

data class ActivityCompletionResponse(
    val studentUserId: String,
    val completedAt: Instant,
)

data class AttendanceResponse(
    val id: Long,
    val courseId: Long,
    val studentUserId: String,
    val attendanceDate: java.time.LocalDate,
    val status: AttendanceStatus,
    val recordedByUserId: String,
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
