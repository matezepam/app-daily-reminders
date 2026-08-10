package com.puce.reminder.repository

import com.puce.reminder.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findAllByReminderIdAndTargetUserIdOrderByNotifyAtAsc(reminderId: Long, targetUserId: String): List<Notification>
    fun findAllByTargetUserIdAndCancelledAtIsNullAndNotifyAtAfterOrderByNotifyAtAsc(targetUserId: String, after: Instant): List<Notification>
    fun findByIdAndTargetUserId(id: Long, targetUserId: String): Notification?
    fun deleteAllByReminderIdAndTargetUserId(reminderId: Long, targetUserId: String): Long

    @Modifying
    @Query("update Notification n set n.cancelledAt = :now where n.reminder.id = :reminderId and n.targetUserId = :userId and n.notifyAt > :now and n.cancelledAt is null")
    fun cancelFuture(reminderId: Long, userId: String, now: Instant): Int
}
