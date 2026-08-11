package com.puce.users.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "users")
class UserProfile(
    @Column(name = "cognito_sub", nullable = false, unique = true, length = 100)
    var cognitoSub: String,
    @Column(nullable = false, unique = true, length = 150)
    var email: String,
    @Column(name = "full_name", nullable = false, length = 120)
    var fullName: String,
    @Column(nullable = false, length = 30)
    var role: String,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
