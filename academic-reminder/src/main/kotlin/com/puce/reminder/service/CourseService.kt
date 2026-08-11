package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.client.UsersClient
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.dto.ExternalUserResponse
import com.puce.reminder.entity.Course
import com.puce.reminder.entity.CourseMembership
import com.puce.reminder.entity.ReminderStatus
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.CourseMapper
import com.puce.reminder.repository.CourseMembershipRepository
import com.puce.reminder.repository.CourseRepository
import com.puce.reminder.repository.ReminderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant

@Service
class CourseService(
    private val courses: CourseRepository,
    private val memberships: CourseMembershipRepository,
    private val reminders: ReminderRepository,
    private val notifications: NotificationService,
    private val access: AccessService,
    private val currentUser: CurrentUser,
    private val usersClient: UsersClient,
    private val audit: AuditService,
    private val mapper: CourseMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val random = SecureRandom()
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun create(request: CourseCreateRequest): CourseResponse {
        val professorUserId = currentUser.id()
        val name = request.name.trim()
        val description = request.description?.trim()?.takeIf(String::isNotEmpty)
        if (courses.existsEquivalentCourse(professorUserId, name, description)) {
            throw ConflictException(DUPLICATE_COURSE_MESSAGE)
        }
        val course = courses.save(Course(
            name = name,
            description = description,
            joinCode = generateCode(name),
            professorUserId = professorUserId,
        ))
        audit.record(professorUserId, "INSERT", "Course", requireNotNull(course.id), current = summary(course))
        log.info("event=course.created | msg=Course created | courseId={}", course.id)
        return toResponse(course)
    }

    @Transactional(readOnly = true)
    fun mine(): List<CourseResponse> {
        val userId = currentUser.id()
        val owned = courses.findAllByProfessorUserIdOrderByCreatedAtDesc(userId)
        val joined = memberships.findAllByStudentUserIdOrderByJoinedAtDesc(userId).map { it.course }
        return (owned + joined).distinctBy { it.id }.map(::toResponse)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): CourseResponse {
        val course = find(id)
        access.requireCanView(course)
        return toResponse(course)
    }

    @Transactional(readOnly = true)
    fun professor(id: Long): ExternalUserResponse {
        val course = find(id)
        access.requireCanView(course)
        return usersClient.getUser(course.professorUserId)
    }

    @Transactional(readOnly = true)
    fun students(id: Long): List<ExternalUserResponse> {
        val course = find(id)
        access.requireCanEdit(course)
        return memberships.findAllByCourseId(id).map { usersClient.getUser(it.studentUserId) }
    }

    @Transactional
    fun join(rawCode: String): CourseResponse {
        val userId = currentUser.id()
        val code = rawCode.trim().uppercase()
        val course = courses.findByJoinCodeIgnoreCase(code) ?: throw NotFoundException("Course code not found")
        if (course.professorUserId == userId) throw ConflictException("The course professor cannot enroll as a student")
        if (memberships.existsByCourseIdAndStudentUserId(requireNotNull(course.id), userId)) throw ConflictException("User is already enrolled in this course")
        memberships.save(CourseMembership(course = course, studentUserId = userId))
        reminders.findAllByCourseIdOrderByDueAtAsc(requireNotNull(course.id))
            .filter { it.status == ReminderStatus.PENDING && it.dueAt.isAfter(Instant.now()) }
            .forEach { notifications.replaceFor(it, userId, setOf(60)) }
        audit.record(userId, "INSERT", "CourseMembership", course.id!!, current = "{\"courseId\":${course.id}}")
        log.info("event=course.joined | msg=Student enrolled in course | courseId={}", course.id)
        return toResponse(course)
    }

    @Transactional
    fun delete(id: Long) {
        val course = find(id)
        access.requireCanEdit(course)
        val previous = summary(course)
        courses.delete(course)
        audit.record(currentUser.id(), "DELETE", "Course", id, previous)
        log.info("event=course.deleted | msg=Course deleted | courseId={}", id)
    }

    fun find(id: Long): Course = courses.findById(id).orElseThrow { NotFoundException("Course not found") }

    private fun generateCode(name: String): String {
        val prefix = name.filter(Char::isLetterOrDigit).uppercase().take(3).padEnd(3, 'C')
        repeat(30) {
            val suffix = (1..5).joinToString("") { alphabet[random.nextInt(alphabet.length)].toString() }
            val candidate = "$prefix-$suffix"
            if (!courses.existsByJoinCode(candidate)) return candidate
        }
        throw ConflictException("A unique course code could not be generated")
    }

    private fun toResponse(course: Course): CourseResponse = mapper.toResponse(
        course,
        memberships.countByCourseId(requireNotNull(course.id)),
        course.professorUserId == currentUser.id(),
    )

    private fun summary(course: Course) = "{\"name\":\"${course.name.replace("\"", "'")}\",\"joinCode\":\"${course.joinCode}\"}"

    companion object {
        const val DUPLICATE_COURSE_MESSAGE =
            "Ya existe un curso con el mismo nombre y descripción. Cambia al menos uno para continuar."
    }
}
