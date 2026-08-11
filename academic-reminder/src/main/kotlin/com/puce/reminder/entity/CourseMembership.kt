package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "course_memberships",
    uniqueConstraints = [UniqueConstraint(name = "uk_membership_course_student", columnNames = ["course_id", "student_user_id"])],
)
class CourseMembership(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course,
    @Column(name = "student_user_id", nullable = false, length = 100)
    var studentUserId: String,
    @Column(name = "joined_at", nullable = false, updatable = false)
    var joinedAt: Instant = Instant.now(),
)
