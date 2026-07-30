package com.puce.reminder.repositories

import com.puce.reminder.entities.Course
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, Long> {
    fun existsByJoinCode(joinCode: String): Boolean
    fun findByJoinCodeIgnoreCase(joinCode: String): Course?
    fun findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId: String): List<Course>
}
