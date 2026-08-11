package com.puce.reminder.mapper

import com.puce.reminder.dto.NotificationResponse
import com.puce.reminder.dto.PriorityCategoryResponse
import com.puce.reminder.dto.ReminderResponse
import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.ReminderStatus
import org.springframework.stereotype.Component

@Component
class ReminderMapper {
    fun toResponse(
        reminder: Reminder,
        status: ReminderStatus,
        category: PriorityCategoryResponse?,
        editable: Boolean,
        notifications: List<NotificationResponse>,
    ) = ReminderResponse(
        id = requireNotNull(reminder.id),
        courseId = reminder.course?.id,
        courseName = reminder.course?.name,
        personal = reminder.course == null,
        title = reminder.title,
        description = reminder.description,
        type = reminder.type,
        dueAt = reminder.dueAt,
        priority = reminder.priority,
        customPriority = category,
        status = status,
        editable = editable,
        createdAt = reminder.createdAt,
        notifications = notifications,
    )
}
