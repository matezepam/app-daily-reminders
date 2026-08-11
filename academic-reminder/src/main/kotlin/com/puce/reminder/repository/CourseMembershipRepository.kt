package com.puce.reminder.repository

import com.puce.reminder.entity.CourseMembership
import org.springframework.data.jpa.repository.JpaRepository

interface CourseMembershipRepository : JpaRepository<CourseMembership, Long> {
    fun existsByCourseIdAndStudentUserId(courseId: Long, studentUserId: String): Boolean
    fun findAllByStudentUserIdOrderByJoinedAtDesc(studentUserId: String): List<CourseMembership>
    fun findAllByCourseId(courseId: Long): List<CourseMembership>
    fun countByCourseId(courseId: Long): Long
}
