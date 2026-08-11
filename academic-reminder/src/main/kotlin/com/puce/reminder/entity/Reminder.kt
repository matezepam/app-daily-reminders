package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "reminders")
class Reminder(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    var course: Course? = null,
    @Column(name = "owner_user_id", length = 100)
    var ownerUserId: String? = null,
    @Column(name = "created_by_user_id", nullable = false, length = 100)
    var createdByUserId: String,
    @Column(nullable = false, length = 100)
    var title: String,
    @Column(length = 500)
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: ReminderType,
    @Column(name = "due_at", nullable = false)
    var dueAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var priority: ReminderPriority = ReminderPriority.MEDIUM,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_category_id")
    var priorityCategory: PriorityCategory? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReminderStatus = ReminderStatus.PENDING,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
