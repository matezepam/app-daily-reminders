package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.PriorityCategoryRequest
import com.puce.reminder.dto.PriorityCategoryResponse
import com.puce.reminder.entity.PriorityCategory
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.mapper.PriorityCategoryMapper
import com.puce.reminder.repository.PriorityCategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PriorityCategoryService(
    private val repository: PriorityCategoryRepository,
    private val currentUser: CurrentUser,
    private val mapper: PriorityCategoryMapper,
    private val audit: AuditService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun list(): List<PriorityCategoryResponse> = repository
        .findAllByOwnerUserIdOrderBySortOrderAscNameAsc(currentUser.id()).map(mapper::toResponse)

    @Transactional
    fun create(request: PriorityCategoryRequest): PriorityCategoryResponse {
        val userId = currentUser.id()
        val name = request.name.trim()
        if (repository.existsByOwnerUserIdAndNameIgnoreCase(userId, name)) {
            throw ConflictException("A priority category with this name already exists")
        }
        val category = repository.save(PriorityCategory(ownerUserId = userId, name = name, color = request.color.uppercase(), sortOrder = request.sortOrder))
        audit.record(userId, "INSERT", "PriorityCategory", requireNotNull(category.id), current = summary(category))
        log.info("event=priority-category.created | msg=Priority category created | categoryId={}", category.id)
        return mapper.toResponse(category)
    }

    @Transactional
    fun delete(id: Long) {
        val category = owned(id)
        val previous = summary(category)
        repository.delete(category)
        audit.record(currentUser.id(), "DELETE", "PriorityCategory", id, previous = previous)
        log.info("event=priority-category.deleted | msg=Priority category deleted | categoryId={}", id)
    }

    fun owned(id: Long): PriorityCategory = repository.findByIdAndOwnerUserId(id, currentUser.id())
        ?: throw NotFoundException("Priority category not found")

    private fun summary(category: PriorityCategory) = "{\"name\":\"${category.name.replace("\"", "'")}\",\"color\":\"${category.color}\"}"
}
