package com.puce.users.repository

import com.puce.users.entity.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long> {
    fun findByCognitoSub(cognitoSub: String): UserProfile?
}
