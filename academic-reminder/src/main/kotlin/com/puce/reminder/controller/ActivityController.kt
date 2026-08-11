package com.puce.reminder.controller

import com.puce.reminder.dto.ActivityRequest
import com.puce.reminder.dto.ActivityResponse
import com.puce.reminder.service.ActivityService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder")
class ActivityController(private val service: ActivityService) {
    @PostMapping("/courses/{courseId}/activities")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@PathVariable courseId: Long, @Valid @RequestBody request: ActivityRequest) = service.create(courseId, request)

    @GetMapping("/courses/{courseId}/activities")
    fun list(@PathVariable courseId: Long): List<ActivityResponse> = service.list(courseId)

    @PutMapping("/activities/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: ActivityRequest) = service.update(id, request)

    @PatchMapping("/activities/{id}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    fun complete(@PathVariable id: Long) = service.complete(id)

    @DeleteMapping("/activities/{id}/completion")
    @PreAuthorize("hasRole('STUDENT')")
    fun uncomplete(@PathVariable id: Long) = service.uncomplete(id)

    @DeleteMapping("/activities/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
