package com.puce.reminder.repository

import com.puce.reminder.entity.PriorityCategory
import org.springframework.data.jpa.repository.JpaRepository

interface PriorityCategoryRepository : JpaRepository<PriorityCategory, Long> {
    fun findAllByOwnerUserIdOrderBySortOrderAscNameAsc(ownerUserId: String): List<PriorityCategory>
    fun existsByOwnerUserIdAndNameIgnoreCase(ownerUserId: String, name: String): Boolean
    fun findByIdAndOwnerUserId(id: Long, ownerUserId: String): PriorityCategory?
}
