package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.client.UsersClient
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.AttendanceRequest
import com.puce.reminder.dto.AttendanceResponse
import com.puce.reminder.entity.Attendance
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.mapper.AttendanceMapper
import com.puce.reminder.repository.AttendanceRepository
import com.puce.reminder.repository.CourseMembershipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AttendanceService(
    private val repository: AttendanceRepository,
    private val memberships: CourseMembershipRepository,
    private val courses: CourseService,
    private val usersClient: UsersClient,
    private val access: AccessService,
    private val currentUser: CurrentUser,
    private val mapper: AttendanceMapper,
    private val audit: AuditService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun record(courseId: Long, request: AttendanceRequest): AttendanceResponse {
        val course = courses.find(courseId)
        access.requireCanEdit(course)
        if (!memberships.existsByCourseIdAndStudentUserId(courseId, request.studentUserId)) {
            throw BadRequestException("The student is not enrolled in this course")
        }
        usersClient.getUser(request.studentUserId)
        val existing = repository.findByCourseIdAndStudentUserIdAndAttendanceDate(courseId, request.studentUserId, request.attendanceDate)
        val previous = existing?.let(::summary)
        val attendance = if (existing == null) {
            repository.save(Attendance(course, request.studentUserId, request.attendanceDate, request.status, currentUser.id()))
        } else {
            existing.status = request.status
            existing.recordedByUserId = currentUser.id()
            existing.updatedAt = Instant.now()
            repository.save(existing)
        }
        audit.record(currentUser.id(), if (existing == null) "INSERT" else "UPDATE", "Attendance", requireNotNull(attendance.id), previous, summary(attendance))
        log.info("event=attendance.recorded | msg=Attendance recorded | attendanceId={} courseId={}", attendance.id, courseId)
        return mapper.toResponse(attendance)
    }

    @Transactional(readOnly = true)
    fun list(courseId: Long): List<AttendanceResponse> {
        val course = courses.find(courseId)
        access.requireCanView(course)
        val attendance = if (access.canEdit(course)) {
            repository.findAllByCourseIdOrderByAttendanceDateDescStudentUserId(courseId)
        } else {
            repository.findAllByCourseIdAndStudentUserIdOrderByAttendanceDateDesc(courseId, currentUser.id())
        }
        return attendance.map(mapper::toResponse)
    }

    private fun summary(attendance: Attendance) = "{\"status\":\"${attendance.status}\",\"studentUserId\":\"${attendance.studentUserId}\"}"
}
