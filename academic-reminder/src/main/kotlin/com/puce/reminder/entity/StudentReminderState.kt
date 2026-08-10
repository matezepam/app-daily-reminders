package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "student_reminder_states",
    uniqueConstraints = [UniqueConstraint(name = "uk_state_reminder_student", columnNames = ["reminder_id", "student_user_id"])],
)
class StudentReminderState(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reminder_id", nullable = false)
    var reminder: Reminder,
    @Column(name = "student_user_id", nullable = false, length = 100)
    var studentUserId: String,
    @Column(name = "completed_at")
    var completedAt: Instant? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "priority_category_id")
    var priorityCategory: PriorityCategory? = null,
)
