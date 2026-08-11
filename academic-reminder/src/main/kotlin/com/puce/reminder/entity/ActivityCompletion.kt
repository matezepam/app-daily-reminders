package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "activity_completions",
    uniqueConstraints = [UniqueConstraint(name = "uk_activity_completion_student", columnNames = ["activity_id", "student_user_id"])],
)
class ActivityCompletion(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    var activity: Activity,
    @Column(name = "student_user_id", nullable = false, length = 100)
    var studentUserId: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "completed_at", nullable = false)
    var completedAt: Instant = Instant.now(),
)
