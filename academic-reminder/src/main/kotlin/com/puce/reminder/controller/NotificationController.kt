package com.puce.reminder.controller

import com.puce.reminder.dto.NotificationRequest
import com.puce.reminder.dto.NotificationResponse
import com.puce.reminder.service.NotificationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder")
class NotificationController(private val service: NotificationService) {
    @GetMapping("/reminders/{reminderId}/notifications")
    fun list(@PathVariable reminderId: Long): List<NotificationResponse> = service.list(reminderId)

    @PostMapping("/reminders/{reminderId}/notifications")
    fun replace(
        @PathVariable reminderId: Long,
        @Valid @RequestBody request: NotificationRequest,
    ): List<NotificationResponse> = service.replace(reminderId, request.offsetsMinutes)

    @GetMapping("/notifications/upcoming")
    fun upcoming(): List<NotificationResponse> = service.upcoming()

    @DeleteMapping("/notifications/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) = service.delete(id)
}
