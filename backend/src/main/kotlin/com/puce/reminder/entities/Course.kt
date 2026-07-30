package com.puce.reminder.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "courses")
class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(length = 255)
    var description: String? = null,

    @Column(name = "join_code", nullable = false, unique = true, updatable = false, length = 9)
    var joinCode: String,

    @Column(name = "owner_user_id", nullable = false, updatable = false, length = 128)
    var ownerUserId: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
