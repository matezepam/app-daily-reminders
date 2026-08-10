package com.puce.reminder.client

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.dto.ExternalUserResponse
import com.puce.reminder.exception.BadGatewayException
import com.puce.reminder.exception.ApiException
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.exception.ServiceUnavailableException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class UsersClient(
    builder: WebClient.Builder,
    @Value("\${app.clients.users.base-url}") baseUrl: String,
    @Value("\${app.clients.users.timeout-ms:3000}") timeoutMs: Long,
    private val currentUser: CurrentUser,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = builder.baseUrl(baseUrl).build()
    private val timeout = Duration.ofMillis(timeoutMs)

    fun getUser(subject: String): ExternalUserResponse {
        log.info("event=users.client.request | msg=GET /users/{}", subject.replace(Regex("[\\r\\n]"), ""))
        return try {
            client.get().uri("/users/{sub}", subject)
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${currentUser.bearerToken()}")
                .retrieve()
                .onStatus({ it == HttpStatus.NOT_FOUND }) { Mono.error(NotFoundException("User profile not found")) }
                .onStatus({ it.is4xxClientError }) { Mono.error(BadGatewayException("Users service rejected the request")) }
                .onStatus({ it.is5xxServerError }) { Mono.error(ServiceUnavailableException("Users service is unavailable")) }
                .bodyToMono(ExternalUserResponse::class.java)
                .timeout(timeout)
                .block() ?: throw ServiceUnavailableException("Users service returned an empty response")
        } catch (exception: NotFoundException) {
            log.warn("event=user.lookup.failed | msg=User profile not found | userSub={}", subject)
            throw exception
        } catch (exception: WebClientRequestException) {
            log.warn("event=users.client.failed | msg=Users service connection failed | reason={}", exception.javaClass.simpleName)
            throw ServiceUnavailableException("Users service is unavailable")
        } catch (exception: ApiException) {
            throw exception
        } catch (exception: RuntimeException) {
            val causes = generateSequence(exception as Throwable?) { it.cause }.toList()
            causes.filterIsInstance<ApiException>().firstOrNull()?.let { throw it }
            if (causes.any { it is java.util.concurrent.TimeoutException }) {
                log.warn("event=users.client.failed | msg=Users service request timed out")
                throw ServiceUnavailableException("Users service request timed out")
            }
            throw BadGatewayException("Users service returned an invalid response")
        } catch (exception: java.util.concurrent.TimeoutException) {
            log.warn("event=users.client.failed | msg=Users service request timed out")
            throw ServiceUnavailableException("Users service request timed out")
        }
    }
}
