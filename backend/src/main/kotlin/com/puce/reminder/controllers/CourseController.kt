package com.puce.reminder.controllers

import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.dto.JoinCourseRequest
import com.puce.reminder.services.CourseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/courses")
class CourseController(private val courseService: CourseService) {
    @GetMapping("/me")
    fun mine(): List<CourseResponse> = courseService.mine()

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    fun create(@Valid @RequestBody request: CourseCreateRequest): CourseResponse = courseService.create(request)

    @PostMapping("/join")
    @PreAuthorize("hasRole('STUDENT')")
    fun join(@Valid @RequestBody request: JoinCourseRequest): CourseResponse = courseService.join(request.code)
}
