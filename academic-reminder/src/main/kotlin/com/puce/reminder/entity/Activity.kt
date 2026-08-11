package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "activities",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_activity_creator_number",
        columnNames = ["created_by_user_id", "activity_number"],
    )],
)
class Activity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course,
    @Column(nullable = false, length = 120)
    var title: String,
    @Column(length = 1000)
    var description: String? = null,
    @Column(name = "due_at", nullable = false)
    var dueAt: Instant,
    @Column(name = "created_by_user_id", nullable = false, length = 100)
    var createdByUserId: String,
    @Column(name = "activity_number", nullable = false)
    var activityNumber: Long = 0,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
