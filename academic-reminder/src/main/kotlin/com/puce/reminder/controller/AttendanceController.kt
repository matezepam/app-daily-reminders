package com.puce.reminder.controller

import com.puce.reminder.dto.AttendanceRequest
import com.puce.reminder.dto.AttendanceResponse
import com.puce.reminder.service.AttendanceService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder/courses/{courseId}/attendance")
class AttendanceController(private val service: AttendanceService) {
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun record(@PathVariable courseId: Long, @Valid @RequestBody request: AttendanceRequest): AttendanceResponse = service.record(courseId, request)

    @GetMapping
    fun list(@PathVariable courseId: Long): List<AttendanceResponse> = service.list(courseId)
}
