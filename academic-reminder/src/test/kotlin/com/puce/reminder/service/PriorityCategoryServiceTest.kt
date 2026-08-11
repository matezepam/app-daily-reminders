package com.puce.reminder.service

import com.puce.reminder.audit.AuditService
import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.PriorityCategoryRequest
import com.puce.reminder.entity.PriorityCategory
import com.puce.reminder.exception.ConflictException
import com.puce.reminder.mapper.PriorityCategoryMapper
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.repository.PriorityCategoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class PriorityCategoryServiceTest {
    private val repository = mock<PriorityCategoryRepository>()
    private val currentUser = mock<CurrentUser>()
    private val audit = mock<AuditService>()
    private val service = PriorityCategoryService(repository, currentUser, PriorityCategoryMapper(), audit)

    @Test
    fun `creates owned category`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.existsByOwnerUserIdAndNameIgnoreCase("student-1", "Critical")).thenReturn(false)
        whenever(repository.save(any<PriorityCategory>())).thenAnswer { it.getArgument<PriorityCategory>(0).apply { id = 4 } }
        val response = service.create(PriorityCategoryRequest(" Critical ", "#aa0000", 1))
        assertEquals("#AA0000", response.color)
    }

    @Test
    fun `rejects duplicate category`() {
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.existsByOwnerUserIdAndNameIgnoreCase("student-1", "Critical")).thenReturn(true)
        assertThrows(ConflictException::class.java) { service.create(PriorityCategoryRequest("Critical", "#AA0000")) }
    }

    @Test
    fun `missing category returns not found`() {
        whenever(currentUser.id()).thenReturn("student-1")
        assertThrows(NotFoundException::class.java) { service.owned(99) }
    }

    @Test
    fun `lists owned categories in repository order`() {
        val category = PriorityCategory(id = 4, ownerUserId = "student-1", name = "Critical", color = "#AA0000", sortOrder = 1)
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findAllByOwnerUserIdOrderBySortOrderAscNameAsc("student-1")).thenReturn(listOf(category))
        assertEquals(4, service.list().single().id)
    }

    @Test
    fun `deletes owned category`() {
        val category = PriorityCategory(id = 4, ownerUserId = "student-1", name = "Critical", color = "#AA0000")
        whenever(currentUser.id()).thenReturn("student-1")
        whenever(repository.findByIdAndOwnerUserId(4, "student-1")).thenReturn(category)
        service.delete(4)
        verify(repository).delete(category)
    }
}
