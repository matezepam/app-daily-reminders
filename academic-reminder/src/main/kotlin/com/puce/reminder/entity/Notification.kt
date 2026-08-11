package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [UniqueConstraint(name = "uk_notification_target_time", columnNames = ["reminder_id", "target_user_id", "notify_at"])],
)
class Notification(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    var reminder: Reminder,
    @Column(name = "target_user_id", nullable = false, length = 100)
    var targetUserId: String,
    @Column(name = "notify_at", nullable = false)
    var notifyAt: Instant,
    @Column(nullable = false)
    var sent: Boolean = false,
    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
