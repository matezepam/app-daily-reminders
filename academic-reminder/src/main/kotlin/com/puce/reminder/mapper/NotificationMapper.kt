package com.puce.reminder.mapper

import com.puce.reminder.dto.NotificationResponse
import com.puce.reminder.entity.Notification
import org.springframework.stereotype.Component

@Component
class NotificationMapper {
    fun toResponse(notification: Notification) = NotificationResponse(
        id = requireNotNull(notification.id),
        reminderId = requireNotNull(notification.reminder.id),
        notifyAt = notification.notifyAt,
        sent = notification.sent,
        cancelled = notification.cancelledAt != null,
    )
}
