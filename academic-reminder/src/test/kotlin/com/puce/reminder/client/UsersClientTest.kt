package com.puce.reminder.client

import com.puce.reminder.config.CurrentUser
import com.puce.reminder.exception.NotFoundException
import com.puce.reminder.exception.BadGatewayException
import com.puce.reminder.exception.ServiceUnavailableException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class UsersClientTest {
    private var server: MockWebServer? = null
    private val currentUser = mock<CurrentUser>()

    @AfterEach
    fun close() {
        server?.shutdown()
    }

    @Test
    fun `forwards bearer token and returns user`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""{"id":1,"cognitoSub":"student-1","email":"student@example.com","fullName":"Student One","role":"STUDENT","createdAt":"2026-08-01T00:00:00Z","updatedAt":"2026-08-01T00:00:00Z"}"""))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)

        assertEquals("student-1", client.getUser("student-1").cognitoSub)
        assertEquals("Bearer access-token", server!!.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `maps users 404 to domain not found`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setResponseCode(404))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)
        assertThrows(NotFoundException::class.java) { client.getUser("missing") }
    }

    @Test
    fun `maps connection failure to service unavailable`() {
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), "http://127.0.0.1:1", 200, currentUser)
        assertThrows(ServiceUnavailableException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps rejected users request to bad gateway`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setResponseCode(400))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)
        assertThrows(BadGatewayException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps users server error to service unavailable`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setResponseCode(500))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)
        assertThrows(ServiceUnavailableException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps empty users response to service unavailable`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setResponseCode(200))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)
        assertThrows(ServiceUnavailableException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps invalid users response to bad gateway`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("not-json"))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 5000, currentUser)
        assertThrows(BadGatewayException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps users timeout to service unavailable`() {
        server = MockWebServer().apply { start() }
        server!!.enqueue(MockResponse().setBody("{}").setBodyDelay(500, TimeUnit.MILLISECONDS))
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val client = UsersClient(WebClient.builder(), server!!.url("/").toString(), 30, currentUser)
        assertThrows(ServiceUnavailableException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `unwraps domain exception from reactive client failure`() {
        whenever(currentUser.bearerToken()).thenReturn("access-token")
        val builder = WebClient.builder().exchangeFunction { Mono.error(RuntimeException(BadGatewayException("Rejected"))) }
        val client = UsersClient(builder, "http://users", 5000, currentUser)
        assertThrows(BadGatewayException::class.java) { client.getUser("student-1") }
    }

    @Test
    fun `maps direct timeout to service unavailable`() {
        whenever(currentUser.bearerToken()).thenAnswer { throw TimeoutException("timeout") }
        val client = UsersClient(WebClient.builder(), "http://users", 5000, currentUser)
        assertThrows(ServiceUnavailableException::class.java) { client.getUser("student-1") }
    }
}
