package com.puce.reminder.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "priority_categories",
    uniqueConstraints = [UniqueConstraint(name = "uk_priority_owner_name", columnNames = ["owner_user_id", "name"])],
)
class PriorityCategory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "owner_user_id", nullable = false, length = 100)
    var ownerUserId: String,
    @Column(nullable = false, length = 40)
    var name: String,
    @Column(nullable = false, length = 7)
    var color: String,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),
)
