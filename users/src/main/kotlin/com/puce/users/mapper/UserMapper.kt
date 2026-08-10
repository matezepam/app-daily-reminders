package com.puce.users.mapper

import com.puce.users.dto.UserResponse
import com.puce.users.entity.UserProfile
import org.springframework.stereotype.Component

@Component
class UserMapper {
    fun toResponse(user: UserProfile) = UserResponse(
        id = requireNotNull(user.id),
        cognitoSub = user.cognitoSub,
        email = user.email,
        fullName = user.fullName,
        role = user.role,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt,
    )
}
