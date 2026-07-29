package com.puce.reminder

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class DailyReminderApplication

fun main(args: Array<String>) {
    runApplication<DailyReminderApplication>(*args)
}

