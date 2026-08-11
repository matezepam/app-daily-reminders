package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.NotificationResponse
import com.puce.reminder.entity.Notification
import com.puce.reminder.entity.Reminder
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.NotificationMapper
import com.puce.reminder.repository.NotificationRepository
import com.puce.reminder.repository.ReminderRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class NotificationService(
    private val repository: NotificationRepository,
    private val reminders: ReminderRepository,
    private val access: AccessService,
    private val currentUser: CurrentUser,
    private val mapper: NotificationMapper,
    private val audit: AuditService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun list(reminderId: Long): List<NotificationResponse> {
        val reminder = reminders.findById(reminderId).orElseThrow { NotFoundException("Reminder not found") }
        access.requireCanView(reminder)
        return repository.findAllByReminderIdAndTargetUserIdOrderByNotifyAtAsc(reminderId, currentUser.id()).map(mapper::toResponse)
    }

    @Transactional(readOnly = true)
    fun upcoming(): List<NotificationResponse> = repository
        .findAllByTargetUserIdAndCancelledAtIsNullAndNotifyAtAfterOrderByNotifyAtAsc(currentUser.id(), Instant.now())
        .map(mapper::toResponse)

    @Transactional
    fun replace(reminderId: Long, offsetsMinutes: Set<Long>): List<NotificationResponse> {
        val reminder = reminders.findById(reminderId).orElseThrow { NotFoundException("Reminder not found") }
        access.requireCanView(reminder)
        return replaceFor(reminder, currentUser.id(), offsetsMinutes)
    }

    @Transactional
    fun replaceFor(reminder: Reminder, userId: String, offsetsMinutes: Set<Long>): List<NotificationResponse> {
        val now = Instant.now()
        if (!reminder.dueAt.isAfter(now)) throw BadRequestException("Notifications cannot be scheduled for an expired reminder")
        repository.deleteAllByReminderIdAndTargetUserId(reminder.id!!, userId)
        repository.flush()
        val created = offsetsMinutes.distinct().sortedDescending().mapNotNull { offset ->
            val notifyAt = reminder.dueAt.minus(offset, ChronoUnit.MINUTES)
            if (notifyAt.isAfter(now)) Notification(reminder = reminder, targetUserId = userId, notifyAt = notifyAt) else null
        }
        val saved = repository.saveAll(created)
        saved.forEach { notification ->
            audit.record(userId, "INSERT", "Notification", requireNotNull(notification.id), current = "{\"reminderId\":${reminder.id}}")
        }
        log.info("event=notification.schedule.replaced | msg=Notification schedule replaced | reminderId={} count={}", reminder.id, saved.size)
        return saved.map(mapper::toResponse)
    }

    @Transactional
    fun cancelFuture(reminderId: Long, userId: String) {
        repository.cancelFuture(reminderId, userId, Instant.now())
        log.info("event=notification.cancelled | msg=Future notifications cancelled | reminderId={}", reminderId)
    }

    @Transactional
    fun delete(id: Long) {
        val notification = repository.findByIdAndTargetUserId(id, currentUser.id())
            ?: throw NotFoundException("Notification not found")
        repository.delete(notification)
        audit.record(currentUser.id(), "DELETE", "Notification", id, previous = "{\"reminderId\":${notification.reminder.id}}")
        log.info("event=notification.deleted | msg=Notification deleted | notificationId={}", id)
    }
}
