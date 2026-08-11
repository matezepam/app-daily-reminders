package com.puce.reminder.repository

import com.puce.reminder.entity.Attendance
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findByCourseIdAndStudentUserIdAndAttendanceDate(courseId: Long, studentUserId: String, attendanceDate: LocalDate): Attendance?
    fun findAllByCourseIdOrderByAttendanceDateDescStudentUserId(courseId: Long): List<Attendance>
    fun findAllByCourseIdAndStudentUserIdOrderByAttendanceDateDesc(courseId: Long, studentUserId: String): List<Attendance>
}
