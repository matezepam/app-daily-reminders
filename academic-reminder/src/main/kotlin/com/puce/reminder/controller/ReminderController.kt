package com.puce.reminder.controller

import com.puce.reminder.dto.*
import com.puce.reminder.service.ReminderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder")
class ReminderController(private val service: ReminderService) {
    @PostMapping("/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    fun createPersonal(@Valid @RequestBody request: ReminderRequest): ReminderResponse = service.createPersonal(request)

    @PostMapping("/courses/{courseId}/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun createForCourse(@PathVariable courseId: Long, @Valid @RequestBody request: ReminderRequest): ReminderResponse =
        service.createForCourse(courseId, request)

    @GetMapping("/reminders/me")
    fun mine(): List<ReminderResponse> = service.mine()

    @GetMapping("/dashboard")
    fun dashboard(): DashboardResponse = service.dashboard()

    @GetMapping("/courses/{courseId}/reminders")
    fun byCourse(@PathVariable courseId: Long): List<ReminderResponse> = service.byCourse(courseId)

    @GetMapping("/reminders/{id}")
    fun get(@PathVariable id: Long): ReminderResponse = service.get(id)

    @PutMapping("/reminders/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: ReminderRequest): ReminderResponse = service.update(id, request)

    @PatchMapping("/reminders/{id}/complete")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    fun complete(@PathVariable id: Long): ReminderResponse = service.complete(id)

    @PatchMapping("/reminders/{id}/priority")
    @PreAuthorize("hasRole('STUDENT')")
    fun overridePriority(
        @PathVariable id: Long,
        @Valid @RequestBody request: ReminderPriorityOverrideRequest,
    ): ReminderResponse = service.setPriorityOverride(id, request.priorityCategoryId)

    @DeleteMapping("/reminders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
