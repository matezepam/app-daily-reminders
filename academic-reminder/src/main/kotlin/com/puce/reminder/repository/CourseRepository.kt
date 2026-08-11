package com.puce.reminder.repository

import com.puce.reminder.entity.Course
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CourseRepository : JpaRepository<Course, Long> {
    fun existsByJoinCode(joinCode: String): Boolean
    fun findByJoinCodeIgnoreCase(joinCode: String): Course?
    fun findAllByProfessorUserIdOrderByCreatedAtDesc(professorUserId: String): List<Course>

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM courses
                WHERE professor_user_id = :professorUserId
                  AND LOWER(BTRIM(name)) = LOWER(BTRIM(:name))
                  AND LOWER(BTRIM(COALESCE(description, ''))) =
                      LOWER(BTRIM(COALESCE(CAST(:description AS TEXT), '')))
            )
        """,
        nativeQuery = true,
    )
    fun existsEquivalentCourse(
        @Param("professorUserId") professorUserId: String,
        @Param("name") name: String,
        @Param("description") description: String?,
    ): Boolean
}
