package com.puce.reminder.repository

import com.puce.reminder.entity.Activity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ActivityRepository : JpaRepository<Activity, Long> {
    fun findAllByCourseIdOrderByDueAtAsc(courseId: Long): List<Activity>

    @Query(
        value = """
            WITH allocated AS (
                INSERT INTO activity_counters (owner_user_id, last_value)
                VALUES (:ownerUserId, 1)
                ON CONFLICT (owner_user_id)
                DO UPDATE SET last_value = activity_counters.last_value + 1
                RETURNING last_value
            )
            SELECT last_value FROM allocated
        """,
        nativeQuery = true,
    )
    fun nextActivityNumber(ownerUserId: String): Long
}
