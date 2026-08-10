package com.puce.reminder.repository

import com.puce.reminder.entity.StudentReminderState
import org.springframework.data.jpa.repository.JpaRepository

interface StudentReminderStateRepository : JpaRepository<StudentReminderState, Long> {
    fun findByReminderIdAndStudentUserId(reminderId: Long, studentUserId: String): StudentReminderState?
    fun findAllByStudentUserIdAndReminderIdIn(studentUserId: String, reminderIds: Collection<Long>): List<StudentReminderState>
}
