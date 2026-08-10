package com.puce.reminder.controller

import com.puce.reminder.dto.PriorityCategoryRequest
import com.puce.reminder.dto.PriorityCategoryResponse
import com.puce.reminder.service.PriorityCategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder/priority-categories")
@PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
class PriorityCategoryController(private val service: PriorityCategoryService) {
    @GetMapping
    fun list(): List<PriorityCategoryResponse> = service.list()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: PriorityCategoryRequest): PriorityCategoryResponse = service.create(request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
