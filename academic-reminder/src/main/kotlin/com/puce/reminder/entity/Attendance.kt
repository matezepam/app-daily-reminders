package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = [UniqueConstraint(name = "uk_attendance_course_student_date", columnNames = ["course_id", "student_user_id", "attendance_date"])],
)
class Attendance(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course,
    @Column(name = "student_user_id", nullable = false, length = 100)
    var studentUserId: String,
    @Column(name = "attendance_date", nullable = false)
    var attendanceDate: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AttendanceStatus,
    @Column(name = "recorded_by_user_id", nullable = false, length = 100)
    var recordedByUserId: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
