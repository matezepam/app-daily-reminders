package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.*
import com.puce.reminder.entity.*
import com.puce.reminder.exception.BadRequestException
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.PriorityCategoryMapper
import com.puce.reminder.mapper.ReminderMapper
import com.puce.reminder.repository.*
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

@Service
class ReminderService(
    private val reminders: ReminderRepository,
    private val memberships: CourseMembershipRepository,
    private val states: StudentReminderStateRepository,
    private val categories: PriorityCategoryRepository,
    private val courseService: CourseService,
    private val notificationService: NotificationService,
    private val access: AccessService,
    private val currentUser: CurrentUser,
    private val audit: AuditService,
    private val mapper: ReminderMapper,
    private val categoryMapper: PriorityCategoryMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    @Transactional
    fun createPersonal(request: ReminderRequest): ReminderResponse {
        val userId = currentUser.id()
        val category = request.priorityCategoryId?.let { ownedCategory(it, userId) }
        val reminder = reminders.save(newReminder(request, ownerUserId = userId, createdByUserId = userId, category = category))
        notificationService.replaceFor(reminder, userId, request.notificationOffsetsMinutes)
        audit.record(userId, "INSERT", "Reminder", requireNotNull(reminder.id), current = summary(reminder))
        log.info("event=reminder.created | msg=Personal reminder created | reminderId={}", reminder.id)
        return toResponse(reminder, userId)
    }

    @Transactional
    fun createForCourse(courseId: Long, request: ReminderRequest): ReminderResponse {
        val course = courseService.find(courseId)
        access.requireCanEdit(course)
        val reminder = reminders.save(newReminder(request, course = course, createdByUserId = currentUser.id()))
        memberships.findAllByCourseId(courseId).forEach {
            notificationService.replaceFor(reminder, it.studentUserId, request.notificationOffsetsMinutes)
        }
        audit.record(currentUser.id(), "INSERT", "Reminder", requireNotNull(reminder.id), current = summary(reminder))
        log.info("event=reminder.created | msg=Course reminder created | reminderId={} courseId={}", reminder.id, courseId)
        return toResponse(reminder, currentUser.id())
    }

    @Transactional(readOnly = true)
    fun mine(): List<ReminderResponse> {
        val userId = currentUser.id()
        val list = if (currentUser.isAdmin()) {
            val ownedCourseIds = courseService.mine().filter { it.ownedByMe }.map { it.id }
            reminders.findAllByOwnerUserIdOrderByDueAtAsc(userId) + ownedCourseIds.flatMap(reminders::findAllByCourseIdOrderByDueAtAsc)
        } else reminders.findVisibleToStudent(userId)
        return list.distinctBy { it.id }.map { toResponse(it, userId, includeNotifications = true) }
    }

    @Transactional(readOnly = true)
    fun byCourse(courseId: Long): List<ReminderResponse> {
        val course = courseService.find(courseId)
        access.requireCanView(course)
        return reminders.findAllByCourseIdOrderByDueAtAsc(courseId).map { toResponse(it, currentUser.id()) }
    }

    @Transactional(readOnly = true)
    fun get(id: Long): ReminderResponse {
        val reminder = find(id)
        access.requireCanView(reminder)
        return toResponse(reminder, currentUser.id(), includeNotifications = true)
    }

    @Transactional
    fun update(id: Long, request: ReminderRequest): ReminderResponse {
        val reminder = find(id)
        access.requireCanEdit(reminder)
        val previous = summary(reminder)
        val category = request.priorityCategoryId?.let { ownedCategory(it, reminder.ownerUserId ?: currentUser.id()) }
        reminder.title = request.title.trim()
        reminder.description = request.description?.trim()?.takeIf { it.isNotEmpty() }
        reminder.type = request.type
        reminder.dueAt = request.dueAt
        reminder.priority = request.priority
        reminder.priorityCategory = category
        reminder.status = ReminderStatus.PENDING
        reminder.updatedAt = Instant.now()
        reminders.save(reminder)

        val targets = if (reminder.course == null) listOf(reminder.ownerUserId!!) else memberships.findAllByCourseId(reminder.course!!.id!!).map { it.studentUserId }
        targets.forEach { notificationService.replaceFor(reminder, it, request.notificationOffsetsMinutes) }
        audit.record(currentUser.id(), "UPDATE", "Reminder", id, previous, summary(reminder))
        log.info("event=reminder.updated | msg=Reminder updated | reminderId={}", id)
        return toResponse(reminder, currentUser.id(), includeNotifications = true)
    }

    @Transactional
    fun complete(id: Long): ReminderResponse {
        val reminder = find(id)
        access.requireCanView(reminder)
        val userId = currentUser.id()
        if (reminder.course == null) {
            if (reminder.ownerUserId != userId) throw ForbiddenException("Only the reminder owner can complete it")
            reminder.status = ReminderStatus.COMPLETED
            reminder.updatedAt = Instant.now()
        } else {
            if (!memberships.existsByCourseIdAndStudentUserId(reminder.course!!.id!!, userId)) {
                throw ForbiddenException("Only an enrolled student can complete this reminder")
            }
            val state = states.findByReminderIdAndStudentUserId(id, userId)
                ?: StudentReminderState(reminder = reminder, studentUserId = userId)
            state.completedAt = Instant.now()
            states.save(state)
        }
        notificationService.cancelFuture(id, userId)
        audit.record(userId, "UPDATE", "Reminder", id, current = "{\"status\":\"COMPLETED\"}")
        log.info("event=reminder.completed | msg=Reminder completed | reminderId={}", id)
        return toResponse(reminder, userId, includeNotifications = true)
    }

    @Transactional
    fun setPriorityOverride(id: Long, categoryId: Long?): ReminderResponse {
        val reminder = find(id)
        access.requireCanView(reminder)
        if (reminder.course == null) throw BadRequestException("Update priority directly on a personal reminder")
        val userId = currentUser.id()
        if (!memberships.existsByCourseIdAndStudentUserId(reminder.course!!.id!!, userId)) {
            throw ForbiddenException("Only enrolled students can customize this priority")
        }
        val state = states.findByReminderIdAndStudentUserId(id, userId)
            ?: StudentReminderState(reminder = reminder, studentUserId = userId)
        state.priorityCategory = categoryId?.let { ownedCategory(it, userId) }
        val savedState = states.save(state)
        audit.record(userId, "UPDATE", "StudentReminderState", requireNotNull(savedState.id), current = "{\"reminderId\":$id,\"priorityCategoryId\":${categoryId ?: "null"}}")
        log.info("event=reminder.priority.updated | msg=Reminder priority override updated | reminderId={}", id)
        return toResponse(reminder, userId)
    }

    @Transactional
    fun delete(id: Long) {
        val reminder = find(id)
        access.requireCanEdit(reminder)
        val previous = summary(reminder)
        reminders.delete(reminder)
        audit.record(currentUser.id(), "DELETE", "Reminder", id, previous)
        log.info("event=reminder.deleted | msg=Reminder deleted | reminderId={}", id)
    }

    @Transactional(readOnly = true)
    fun dashboard(): DashboardResponse {
        val all = mine()
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = now.atZone(zone).toLocalDate()
        val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
        return DashboardResponse(
            pendingToday = all.count { it.status == ReminderStatus.PENDING && it.dueAt.atZone(zone).toLocalDate() == today },
            upcoming = all.count { it.status == ReminderStatus.PENDING && it.dueAt.isAfter(now) },
            completedThisMonth = all.count { it.status == ReminderStatus.COMPLETED && !it.createdAt.atZone(zone).toLocalDate().isBefore(monthStart) },
            reminders = all.sortedBy { it.dueAt },
        )
    }

    private fun find(id: Long) = reminders.findById(id).orElseThrow { NotFoundException("Reminder not found") }

    private fun ownedCategory(id: Long, userId: String) = categories.findByIdAndOwnerUserId(id, userId)
        ?: throw NotFoundException("Priority category not found")

    private fun newReminder(
        request: ReminderRequest,
        course: Course? = null,
        ownerUserId: String? = null,
        createdByUserId: String,
        category: PriorityCategory? = null,
    ) = Reminder(
        course = course,
        ownerUserId = ownerUserId,
        createdByUserId = createdByUserId,
        title = request.title.trim(),
        description = request.description?.trim()?.takeIf { it.isNotEmpty() },
        type = request.type,
        dueAt = request.dueAt,
        priority = request.priority,
        priorityCategory = category,
    )

    private fun toResponse(reminder: Reminder, userId: String, includeNotifications: Boolean = false): ReminderResponse {
        val state = if (reminder.course != null) states.findByReminderIdAndStudentUserId(reminder.id!!, userId) else null
        val effectiveStatus = when {
            state?.completedAt != null -> ReminderStatus.COMPLETED
            else -> reminder.status
        }
        val category = (state?.priorityCategory ?: reminder.priorityCategory)?.let(categoryMapper::toResponse)
        return mapper.toResponse(
            reminder = reminder,
            status = effectiveStatus,
            category = category,
            editable = reminder.ownerUserId == userId || reminder.course?.professorUserId == userId,
            notifications = if (includeNotifications) notificationService.list(reminder.id!!) else emptyList(),
        )
    }

    private fun summary(reminder: Reminder) = "{\"title\":\"${reminder.title.replace("\"", "'")}\",\"status\":\"${reminder.status}\"}"
}
