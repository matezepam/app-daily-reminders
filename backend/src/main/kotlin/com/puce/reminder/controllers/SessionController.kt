package com.puce.reminder.controllers

import com.puce.reminder.dto.SessionResponse
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/session")
class SessionController {
    @GetMapping
    fun currentSession(authentication: JwtAuthenticationToken): SessionResponse {
        val userId = authentication.token.subject ?: authentication.name
        return SessionResponse(
            userId = userId,
            username = authentication.token.getClaimAsString("username") ?: userId,
            roles = authentication.authorities
                .mapNotNull { authority -> authority.authority }
                .filter { authority -> authority.startsWith("ROLE_") }
                .map { authority -> authority.removePrefix("ROLE_") }
                .toSortedSet(),
        )
    }
}
