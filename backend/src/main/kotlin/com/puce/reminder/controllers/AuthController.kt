package com.puce.reminder.controllers

import com.puce.reminder.dto.AuthenticationResponse
import com.puce.reminder.dto.ConfirmRegistrationRequest
import com.puce.reminder.dto.LoginRequest
import com.puce.reminder.dto.MessageResponse
import com.puce.reminder.dto.RegisterRequest
import com.puce.reminder.dto.RegistrationResponse
import com.puce.reminder.services.CognitoAuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: CognitoAuthService,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<RegistrationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/confirm")
    fun confirmRegistration(
        @Valid @RequestBody request: ConfirmRegistrationRequest,
    ): ResponseEntity<MessageResponse> {
        authService.confirmRegistration(request)
        return ResponseEntity.ok(MessageResponse("Account confirmed successfully"))
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthenticationResponse =
        authService.login(request)
}
