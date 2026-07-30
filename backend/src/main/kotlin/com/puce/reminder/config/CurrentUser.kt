package com.puce.reminder.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CurrentUser {
    fun id(): String {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: error("No authenticated JWT user is available")
        return authentication.token.subject ?: authentication.name
    }
}
