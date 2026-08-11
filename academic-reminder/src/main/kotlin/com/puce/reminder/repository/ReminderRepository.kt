package com.puce.reminder.repository

import com.puce.reminder.entity.Reminder
import com.puce.reminder.entity.ReminderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant

interface ReminderRepository : JpaRepository<Reminder, Long> {
    fun findAllByCourseIdOrderByDueAtAsc(courseId: Long): List<Reminder>
    fun findAllByOwnerUserIdOrderByDueAtAsc(ownerUserId: String): List<Reminder>

    @Query("""
        select r from Reminder r
        where r.ownerUserId = :userId
           or r.course.id in (
               select m.course.id from CourseMembership m where m.studentUserId = :userId
           )
        order by r.dueAt asc
    """)
    fun findVisibleToStudent(userId: String): List<Reminder>

    @Modifying(clearAutomatically = true)
    @Query("update Reminder r set r.status = :expired, r.updatedAt = :now where r.status = :pending and r.dueAt < :now")
    fun expirePending(now: Instant, pending: ReminderStatus, expired: ReminderStatus): Int
}
