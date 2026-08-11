package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.ActivityRequest
import com.puce.reminder.dto.ActivityResponse
import com.puce.reminder.entity.Activity
import com.puce.reminder.entity.ActivityCompletion
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.ActivityMapper
import com.puce.reminder.repository.ActivityCompletionRepository
import com.puce.reminder.repository.ActivityRepository
import com.puce.reminder.repository.CourseMembershipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class ActivityService(
    private val repository: ActivityRepository,
    private val completions: ActivityCompletionRepository,
    private val memberships: CourseMembershipRepository,
    private val courses: CourseService,
    private val access: AccessService,
    private val currentUser: CurrentUser,
    private val mapper: ActivityMapper,
    private val audit: AuditService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun create(courseId: Long, request: ActivityRequest): ActivityResponse {
        val course = courses.find(courseId)
        access.requireCanEdit(course)
        val userId = currentUser.id()
        val activityNumber = repository.nextActivityNumber(userId)
        val activity = repository.save(Activity(
            course = course,
            title = request.title.trim(),
            description = request.description?.trim()?.takeIf(String::isNotEmpty),
            dueAt = request.dueAt,
            createdByUserId = userId,
            activityNumber = activityNumber,
        ))
        audit.record(currentUser.id(), "INSERT", "Activity", requireNotNull(activity.id), current = summary(activity))
        log.info("event=activity.created | msg=Activity created | activityId={} activityNumber={} courseId={}", activity.id, activity.activityNumber, courseId)
        return mapper.toResponse(activity)
    }

    @Transactional(readOnly = true)
    fun list(courseId: Long): List<ActivityResponse> {
        val course = courses.find(courseId)
        access.requireCanView(course)
        val userId = currentUser.id()
        val completionsByActivityId = completions.findAllForCourseAndStudent(courseId, userId)
            .associateBy { requireNotNull(it.activity.id) }
        return repository.findAllByCourseIdOrderByDueAtAsc(courseId)
            .map { mapper.toResponse(it, completionsByActivityId[it.id]) }
    }

    @Transactional
    fun update(id: Long, request: ActivityRequest): ActivityResponse {
        val activity = find(id)
        access.requireCanEdit(activity.course)
        val previous = summary(activity)
        activity.title = request.title.trim()
        activity.description = request.description?.trim()?.takeIf(String::isNotEmpty)
        activity.dueAt = request.dueAt
        activity.updatedAt = Instant.now()
        repository.save(activity)
        audit.record(currentUser.id(), "UPDATE", "Activity", id, previous, summary(activity))
        log.info("event=activity.updated | msg=Activity updated | activityId={}", id)
        return mapper.toResponse(activity)
    }

    @Transactional
    fun complete(id: Long): ActivityResponse {
        val activity = find(id)
        access.requireCanView(activity.course)
        val userId = currentUser.id()
        if (!memberships.existsByCourseIdAndStudentUserId(requireNotNull(activity.course.id), userId)) {
            throw ForbiddenException("Only enrolled students can complete an activity")
        }
        if (completions.existsByActivityIdAndStudentUserId(id, userId)) throw ConflictException("Activity is already completed")
        val completion = completions.save(ActivityCompletion(activity, userId))
        audit.record(userId, "INSERT", "ActivityCompletion", requireNotNull(completion.id), current = "{\"activityId\":$id}")
        log.info("event=activity.completed | msg=Activity completed | activityId={}", id)
        return mapper.toResponse(activity, completion)
    }

    @Transactional
    fun delete(id: Long) {
        val activity = find(id)
        access.requireCanEdit(activity.course)
        val previous = summary(activity)
        repository.delete(activity)
        audit.record(currentUser.id(), "DELETE", "Activity", id, previous)
        log.info("event=activity.deleted | msg=Activity deleted | activityId={}", id)
    }

    private fun find(id: Long) = repository.findById(id).orElseThrow { NotFoundException("Activity not found") }
    private fun summary(activity: Activity) = "{\"title\":\"${activity.title.replace("\"", "'")}\",\"courseId\":${activity.course.id}}"
}
