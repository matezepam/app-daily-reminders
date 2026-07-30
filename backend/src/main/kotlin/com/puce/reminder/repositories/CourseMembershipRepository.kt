package com.puce.reminder.repositories

import com.puce.reminder.entities.CourseMembership
import org.springframework.data.jpa.repository.JpaRepository

interface CourseMembershipRepository : JpaRepository<CourseMembership, Long> {
    fun existsByCourseIdAndStudentUserId(courseId: Long, studentUserId: String): Boolean
    fun findAllByStudentUserIdOrderByJoinedAtDesc(studentUserId: String): List<CourseMembership>
    fun countByCourseId(courseId: Long): Long
}
