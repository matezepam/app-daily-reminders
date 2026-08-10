package com.puce.users.service

import com.puce.users.audit.AuditService
import com.puce.users.client.CognitoProfile
import com.puce.users.client.CognitoProfileClient
import com.puce.users.dto.UpdateUserRequest
import com.puce.users.entity.UserProfile
import com.puce.users.exception.UserNotFoundException
import com.puce.users.mapper.UserMapper
import com.puce.users.repository.UserProfileRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class UserServiceTest {
    private val repository = mock<UserProfileRepository>()
    private val audit = mock<AuditService>()
    private val cognito = mock<CognitoProfileClient>()
    private val service = UserService(repository, UserMapper(), audit, cognito)

    @Test
    fun `creates profile from jwt claims`() {
        whenever(repository.findByCognitoSub("sub-1")).thenReturn(null)
        whenever(repository.save(any<UserProfile>())).thenAnswer { invocation -> invocation.getArgument<UserProfile>(0).apply { id = 1 } }

        val token = jwt("sub-1", "STUDENT", "Student One")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("sub-1@example.com", "Student One"))

        val response = service.synchronize(token)

        assertEquals("Student One", response.fullName)
        assertEquals("STUDENT", response.role)
        verify(audit).record(eq("sub-1"), eq("INSERT"), eq("1"), isNull(), any())
    }

    @Test
    fun `updates synchronized claims and writes audit`() {
        val existing = UserProfile("sub-2", "old@example.com", "Old Name", "STUDENT", id = 2)
        whenever(repository.findByCognitoSub("sub-2")).thenReturn(existing)
        whenever(repository.save(existing)).thenReturn(existing)

        val token = jwt("sub-2", "ADMIN", "Professor Two")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("sub-2@example.com", "Professor Two"))

        val response = service.synchronize(token)

        assertEquals("ADMIN", response.role)
        assertEquals("Old Name", response.fullName)
        verify(audit).record(eq("sub-2"), eq("UPDATE"), eq("2"), any(), any())
    }

    @Test
    fun `updates own full name`() {
        val existing = UserProfile("sub-3", "sub-3@example.com", "Student Three", "STUDENT", id = 3)
        whenever(repository.findByCognitoSub("sub-3")).thenReturn(existing)
        whenever(repository.save(existing)).thenReturn(existing)

        val token = jwt("sub-3", "STUDENT", "Student Three")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("sub-3@example.com", "Student Three"))

        val response = service.update(token, UpdateUserRequest("Updated Name"))

        assertEquals("Updated Name", response.fullName)
        verify(audit).record(eq("sub-3"), eq("UPDATE"), eq("3"), any(), any())
    }

    @Test
    fun `keeps edited full name during later cognito synchronization`() {
        val existing = UserProfile("sub-8", "sub-8@example.com", "Original Name", "STUDENT", id = 8)
        whenever(repository.findByCognitoSub("sub-8")).thenReturn(existing)
        whenever(repository.save(existing)).thenReturn(existing)
        val token = jwt("sub-8", "STUDENT", "Original Name")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("sub-8@example.com", "Original Name"))

        service.update(token, UpdateUserRequest("Persistent Name"))
        val synchronized = service.synchronize(token)

        assertEquals("Persistent Name", synchronized.fullName)
    }

    @Test
    fun `missing subject returns not found`() {
        whenever(repository.findByCognitoSub("missing")).thenReturn(null)
        assertThrows(UserNotFoundException::class.java) { service.findBySubject("missing") }
    }

    @Test
    fun `accepts administrator group`() {
        whenever(repository.findByCognitoSub("sub-4")).thenReturn(null)
        whenever(repository.save(any<UserProfile>())).thenAnswer { it.getArgument<UserProfile>(0).apply { id = 4 } }
        val token = jwt("sub-4", "ADMIN", "Administrator")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("admin@example.com", "Administrator"))

        val response = service.synchronize(token)

        assertEquals("Administrator", response.fullName)
        assertEquals("admin@example.com", response.email)
        assertEquals("ADMIN", response.role)
    }

    @Test
    fun `rejects token without a recognized group`() {
        val token = jwt("sub-5", "GUEST", "Guest")

        assertThrows(AccessDeniedException::class.java) { service.synchronize(token) }
        verifyNoInteractions(cognito, repository, audit)
    }

    @Test
    fun `does not persist unchanged synchronized profile`() {
        val existing = UserProfile("sub-6", "sub-6@example.com", "Student Six", "STUDENT", id = 6)
        whenever(repository.findByCognitoSub("sub-6")).thenReturn(existing)

        val token = jwt("sub-6", "STUDENT", "Student Six")
        whenever(cognito.profile(token)).thenReturn(CognitoProfile("sub-6@example.com", "Student Six"))

        val response = service.synchronize(token)

        assertEquals(6, response.id)
        verify(repository, never()).save(existing)
        verifyNoInteractions(audit)
    }

    @Test
    fun `returns existing profile by subject`() {
        val existing = UserProfile("sub-7", "sub-7@example.com", "Student Seven", "STUDENT", id = 7)
        whenever(repository.findByCognitoSub("sub-7")).thenReturn(existing)
        assertEquals(7, service.findBySubject("sub-7").id)
    }

    private fun jwt(subject: String, group: String, name: String): Jwt = Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject(subject)
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(60))
        .claim("email", "$subject@example.com")
        .claim("name", name)
        .claim("cognito:groups", listOf(group))
        .build()
}
