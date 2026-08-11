package com.puce.reminder.service

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.entity.Course
import com.puce.reminder.entity.Reminder
import com.puce.reminder.exception.ForbiddenException
import com.puce.reminder.repository.CourseMembershipRepository
import org.springframework.stereotype.Service

@Service
class AccessService(
    private val currentUser: CurrentUser,
    private val memberships: CourseMembershipRepository,
) {
    fun canView(course: Course): Boolean =
        course.professorUserId == currentUser.id() ||
            memberships.existsByCourseIdAndStudentUserId(course.id!!, currentUser.id())

    fun requireCanView(course: Course) {
        if (!canView(course)) throw ForbiddenException("User does not belong to this course")
    }

    fun canEdit(course: Course): Boolean = course.professorUserId == currentUser.id()

    fun requireCanEdit(course: Course) {
        if (!canEdit(course)) throw ForbiddenException("Only the course professor can modify this course")
    }

    fun requireCanView(reminder: Reminder) {
        if (reminder.course != null) requireCanView(reminder.course!!)
        else if (reminder.ownerUserId != currentUser.id()) {
            throw ForbiddenException("This reminder belongs to another user")
        }
    }

    fun requireCanEdit(reminder: Reminder) {
        if (reminder.course != null) requireCanEdit(reminder.course!!)
        else if (reminder.ownerUserId != currentUser.id()) {
            throw ForbiddenException("This reminder belongs to another user")
        }
    }
}
