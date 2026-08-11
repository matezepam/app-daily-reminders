package com.puce.users.service

import com.puce.users.audit.AuditService
import com.puce.users.client.CognitoProfileClient
import com.puce.users.dto.UpdateUserRequest
import com.puce.users.dto.UserResponse
import com.puce.users.entity.UserProfile
import com.puce.users.exception.UserNotFoundException
import com.puce.users.mapper.UserMapper
import com.puce.users.repository.UserProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserService(
    private val repository: UserProfileRepository,
    private val mapper: UserMapper,
    private val audit: AuditService,
    private val cognito: CognitoProfileClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun synchronize(jwt: Jwt): UserResponse {
        val subject = requireNotNull(jwt.subject) { "The access token does not contain sub" }
        val role = role(jwt)
        val profile = cognito.profile(jwt)
        val email = profile.email
        val name = profile.fullName
        val existing = repository.findByCognitoSub(subject)
        if (existing == null) {
            val saved = repository.save(UserProfile(subject, email, name, role))
            audit.record(subject, "INSERT", requireNotNull(saved.id).toString(), null, summary(saved))
            log.info("event=user.created | msg=User profile synchronized | userId={}", saved.id)
            return mapper.toResponse(saved)
        }
        val previous = summary(existing)
        // Once created, the profile service is the source of truth for the editable name.
        // Re-synchronizing Cognito identity data must not undo PUT /users/me.
        if (existing.email != email || existing.role != role) {
            existing.email = email
            existing.role = role
            existing.updatedAt = Instant.now()
            repository.save(existing)
            audit.record(subject, "UPDATE", requireNotNull(existing.id).toString(), previous, summary(existing))
            log.info("event=user.updated | msg=User profile synchronized | userId={}", existing.id)
        }
        return mapper.toResponse(existing)
    }

    @Transactional
    fun update(jwt: Jwt, request: UpdateUserRequest): UserResponse {
        synchronize(jwt)
        val subject = requireNotNull(jwt.subject) { "The access token does not contain sub" }
        val user = repository.findByCognitoSub(subject) ?: throw UserNotFoundException("User profile not found")
        val previous = summary(user)
        user.fullName = request.fullName.trim()
        user.updatedAt = Instant.now()
        repository.save(user)
        audit.record(subject, "UPDATE", requireNotNull(user.id).toString(), previous, summary(user))
        log.info("event=user.updated | msg=User profile updated | userId={}", user.id)
        return mapper.toResponse(user)
    }

    @Transactional(readOnly = true)
    fun findBySubject(subject: String): UserResponse = mapper.toResponse(
        repository.findByCognitoSub(subject) ?: throw UserNotFoundException("User profile not found"),
    )

    private fun role(jwt: Jwt): String {
        val groups = (jwt.getClaimAsStringList("cognito:groups") ?: emptyList())
            .map(String::uppercase)
            .filter { it == "ADMIN" || it == "STUDENT" }
            .distinct()
        return when (groups) {
            listOf("ADMIN") -> "ADMIN"
            listOf("STUDENT") -> "STUDENT"
            else -> throw AccessDeniedException("The access token must contain exactly one application role")
        }
    }

    private fun summary(user: UserProfile) = "{\"fullName\":\"${user.fullName.replace("\"", "'")}\",\"role\":\"${user.role}\"}"
}
