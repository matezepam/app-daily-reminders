package com.puce.users.mapper

import com.puce.users.entity.UserProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UserMapperTest {
    @Test
    fun `maps user profile to response`() {
        val profile = UserProfile("sub-1", "user@example.com", "User One", "STUDENT", id = 7)
        val response = UserMapper().toResponse(profile)
        assertEquals(7, response.id)
        assertEquals("sub-1", response.cognitoSub)
        assertEquals("User One", response.fullName)
    }
}
