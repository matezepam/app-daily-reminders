package com.puce.users.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(-99)
class RequestLoggingFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        MDC.put("sub", authentication?.token?.subject ?: "anonimo")
        try {
            log.info("event=http.request | msg={} {}", request.method, request.requestURI.replace(Regex("[\\r\\n]"), ""))
            filterChain.doFilter(request, response)
            log.info("event=http.response | msg={} {} {}", response.status, request.method, request.requestURI.replace(Regex("[\\r\\n]"), ""))
        } finally {
            MDC.remove("sub")
        }
    }
}
