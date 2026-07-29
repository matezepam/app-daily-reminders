package com.puce.reminder.security

enum class CognitoRole {
    STUDENT,
    TEACHER,
    ADMIN;

    companion object {
        fun fromGroup(group: String): CognitoRole? =
            entries.firstOrNull { role -> role.name.equals(group.trim(), ignoreCase = true) }
    }
}
