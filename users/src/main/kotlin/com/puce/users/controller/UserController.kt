package com.puce.users.controller

import com.puce.users.dto.UpdateUserRequest
import com.puce.users.dto.UserResponse
import com.puce.users.service.UserService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users")
class UserController(private val service: UserService) {
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse = service.synchronize(jwt)

    @PutMapping("/me")
    fun update(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: UpdateUserRequest): UserResponse = service.update(jwt, request)

    @GetMapping("/{sub}")
    @PreAuthorize("hasAnyRole('STUDENT','ADMIN')")
    fun get(@PathVariable sub: String): UserResponse = service.findBySubject(sub)
}
