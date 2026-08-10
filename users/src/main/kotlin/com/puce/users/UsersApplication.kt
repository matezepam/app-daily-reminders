package com.puce.users

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class UsersApplication

fun main(args: Array<String>) {
    runApplication<UsersApplication>(*args)
}
