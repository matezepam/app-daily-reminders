package com.puce.reminder.mapper

import com.puce.reminder.dto.AttendanceResponse
import com.puce.reminder.entity.Attendance
import org.springframework.stereotype.Component

@Component
class AttendanceMapper {
    fun toResponse(attendance: Attendance) = AttendanceResponse(
        id = requireNotNull(attendance.id),
        courseId = requireNotNull(attendance.course.id),
        studentUserId = attendance.studentUserId,
        attendanceDate = attendance.attendanceDate,
        status = attendance.status,
        recordedByUserId = attendance.recordedByUserId,
        createdAt = attendance.createdAt,
        updatedAt = attendance.updatedAt,
    )
}
