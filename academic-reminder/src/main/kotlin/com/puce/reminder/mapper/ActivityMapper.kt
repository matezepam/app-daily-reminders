package com.puce.reminder.mapper

import com.puce.reminder.dto.ActivityResponse
import com.puce.reminder.dto.ActivityCompletionResponse
import com.puce.reminder.entity.Activity
import com.puce.reminder.entity.ActivityCompletion
import org.springframework.stereotype.Component

@Component
class ActivityMapper {
    fun toResponse(
        activity: Activity,
        completion: ActivityCompletion? = null,
        courseCompletions: List<ActivityCompletion> = emptyList(),
    ) = ActivityResponse(
        id = requireNotNull(activity.id),
        courseId = requireNotNull(activity.course.id),
        title = activity.title,
        description = activity.description,
        dueAt = activity.dueAt,
        createdByUserId = activity.createdByUserId,
        completed = completion != null,
        createdAt = activity.createdAt,
        updatedAt = activity.updatedAt,
        activityNumber = activity.activityNumber,
        completedAt = completion?.completedAt,
        completionCount = courseCompletions.size,
        completions = courseCompletions.map {
            ActivityCompletionResponse(
                studentUserId = it.studentUserId,
                completedAt = it.completedAt,
            )
        },
    )
}
