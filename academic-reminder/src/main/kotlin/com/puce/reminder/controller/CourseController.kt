package com.puce.reminder.controller

import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.dto.JoinCourseRequest
import com.puce.reminder.dto.ExternalUserResponse
import com.puce.reminder.service.CourseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/academic-reminder/courses")
class CourseController(private val service: CourseService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: CourseCreateRequest): CourseResponse = service.create(request)

    @GetMapping("/me")
    fun mine(): List<CourseResponse> = service.mine()

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): CourseResponse = service.get(id)

    @GetMapping("/{id}/professor")
    fun professor(@PathVariable id: Long): ExternalUserResponse = service.professor(id)

    @GetMapping("/{id}/students")
    @PreAuthorize("hasRole('ADMIN')")
    fun students(@PathVariable id: Long): List<ExternalUserResponse> = service.students(id)

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    fun join(@Valid @RequestBody request: JoinCourseRequest): CourseResponse = service.join(request.code)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
