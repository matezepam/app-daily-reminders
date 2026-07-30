package com.puce.reminder.services

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.CourseCreateRequest
import com.puce.reminder.dto.CourseResponse
import com.puce.reminder.entities.Course
import com.puce.reminder.exceptions.ApiException
import com.puce.reminder.repositories.CourseRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.text.Normalizer

@Service
class CourseService(
    private val courses: CourseRepository,
    private val currentUser: CurrentUser,
) {
    private val random = SecureRandom()

    @Transactional
    fun create(request: CourseCreateRequest): CourseResponse {
        val course = courses.save(
            Course(
                name = request.name.trim(),
                description = request.description?.trim()?.takeIf(String::isNotEmpty),
                joinCode = generateUniqueCode(request.name),
                ownerUserId = currentUser.id(),
            ),
        )
        return course.toResponse()
    }

    private fun generateUniqueCode(courseName: String): String {
        val prefix = Normalizer.normalize(courseName, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .uppercase()
            .filter(Char::isLetterOrDigit)
            .take(3)
            .padEnd(3, 'C')

        repeat(MAX_CODE_ATTEMPTS) {
            val suffix = buildString(CODE_SUFFIX_LENGTH) {
                repeat(CODE_SUFFIX_LENGTH) { append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]) }
            }
            val candidate = "$prefix-$suffix"
            if (!courses.existsByJoinCode(candidate)) return candidate
        }

        throw ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "A unique course code could not be generated")
    }

    private fun Course.toResponse() = CourseResponse(
        id = requireNotNull(id),
        name = name,
        description = description,
        joinCode = joinCode,
        ownerUserId = ownerUserId,
        createdAt = createdAt,
    )

    private companion object {
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val CODE_SUFFIX_LENGTH = 5
        const val MAX_CODE_ATTEMPTS = 50
    }
}
