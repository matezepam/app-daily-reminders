package com.puce.reminder.mapper

import com.puce.reminder.dto.PriorityCategoryResponse
import com.puce.reminder.entity.PriorityCategory
import org.springframework.stereotype.Component

@Component
class PriorityCategoryMapper {
    fun toResponse(category: PriorityCategory) = PriorityCategoryResponse(
        id = requireNotNull(category.id),
        name = category.name,
        color = category.color,
        sortOrder = category.sortOrder,
    )
}
