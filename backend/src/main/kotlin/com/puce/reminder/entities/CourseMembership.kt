package com.puce.reminder.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
    name = "course_memberships",
    uniqueConstraints = [UniqueConstraint(name = "uk_course_membership_student", columnNames = ["course_id", "student_user_id"])],
)
class CourseMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, updatable = false)
    var course: Course,

    @Column(name = "student_user_id", nullable = false, updatable = false, length = 128)
    var studentUserId: String,

    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),
)
