package com.puce.reminder.service

import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.repository.ReminderRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class ExpirationScheduler(private val reminders: ReminderRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.scheduling.expiration-delay-ms:60000}")
    @Transactional
    fun expirePendingReminders() {
        val count = reminders.expirePending(Instant.now(), ReminderStatus.PENDING, ReminderStatus.EXPIRED)
        if (count > 0) log.info("event=reminder.expired | msg=Pending reminders expired | count={}", count)
    }
}
