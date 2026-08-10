package com.puce.users.audit

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "audit_log")
class AuditLog(
    @Column(name = "user_sub", nullable = false, length = 100) var userSub: String,
    @Column(nullable = false, length = 20) var action: String,
    @Column(name = "entity_type", nullable = false, length = 80) var entityType: String,
    @Column(name = "entity_id", nullable = false, length = 100) var entityId: String,
    @Column(name = "previous_values", columnDefinition = "TEXT") var previousValues: String? = null,
    @Column(name = "new_values", columnDefinition = "TEXT") var newValues: String? = null,
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
)
