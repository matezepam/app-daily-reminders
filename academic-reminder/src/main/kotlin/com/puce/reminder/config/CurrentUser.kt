package com.puce.reminder.config

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

@Component
class CurrentUser {
    fun id(): String = token().token.subject ?: error("The access token does not contain sub")
    fun bearerToken(): String = token().token.tokenValue
    fun hasRole(role: String): Boolean = token().authorities.any { it.authority == "ROLE_${role.uppercase()}" }
    fun isAdmin(): Boolean = hasRole("ADMIN")

    private fun token(): JwtAuthenticationToken =
        SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: error("No authenticated JWT user is available")
}
