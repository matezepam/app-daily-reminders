package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "courses")
class Course(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(length = 255)
    var description: String? = null,
    @Column(name = "join_code", nullable = false, unique = true, length = 20)
    var joinCode: String,
    @Column(name = "professor_user_id", nullable = false, length = 100)
    var professorUserId: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
