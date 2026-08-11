package com.puce.reminder.mapper

import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.entity.Course
import org.springframework.stereotype.Component

@Component
class CourseMapper {
    fun toResponse(course: Course, memberCount: Long, ownedByCurrentUser: Boolean) = CourseResponse(
        id = requireNotNull(course.id),
        name = course.name,
        description = course.description,
        joinCode = course.joinCode,
        professorUserId = course.professorUserId,
        memberCount = memberCount,
        createdAt = course.createdAt,
        ownedByMe = ownedByCurrentUser,
    )
}
