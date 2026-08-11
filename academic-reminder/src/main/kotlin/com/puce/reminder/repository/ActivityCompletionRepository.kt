package com.puce.reminder.repository

import com.puce.reminder.entity.ActivityCompletion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ActivityCompletionRepository : JpaRepository<ActivityCompletion, Long> {
    fun findByActivityIdAndStudentUserId(activityId: Long, studentUserId: String): ActivityCompletion?
    fun existsByActivityIdAndStudentUserId(activityId: Long, studentUserId: String): Boolean

    @Query(
        """
        select completion
        from ActivityCompletion completion
        join fetch completion.activity activity
        where activity.course.id = :courseId
          and completion.studentUserId = :studentUserId
        """,
    )
    fun findAllForCourseAndStudent(
        @Param("courseId") courseId: Long,
        @Param("studentUserId") studentUserId: String,
    ): List<ActivityCompletion>

    @Query(
        """
        select completion
        from ActivityCompletion completion
        join fetch completion.activity activity
        where activity.course.id = :courseId
        order by completion.completedAt desc
        """,
    )
    fun findAllForCourse(@Param("courseId") courseId: Long): List<ActivityCompletion>
}
