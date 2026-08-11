package com.puce.users.exception

import com.puce.users.dto.ApiError
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(UserNotFoundException::class)
    fun notFound(ex: UserNotFoundException, request: HttpServletRequest) = response(HttpStatus.NOT_FOUND, ex.message, request)

    @ExceptionHandler(UserConflictException::class, DataIntegrityViolationException::class)
    fun conflict(ex: Exception, request: HttpServletRequest) = response(
        HttpStatus.CONFLICT,
        if (ex is UserConflictException) ex.message else "User data conflicts with an existing profile",
        request,
    )

    @ExceptionHandler(UserAuthenticationException::class)
    fun unauthorized(ex: UserAuthenticationException, request: HttpServletRequest) = response(HttpStatus.UNAUTHORIZED, ex.message, request)

    @ExceptionHandler(IdentityProviderException::class)
    fun identityProvider(ex: IdentityProviderException, request: HttpServletRequest) = response(HttpStatus.SERVICE_UNAVAILABLE, ex.message, request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.badRequest().body(ApiError(status = 400, error = "Bad Request", message = "Request validation failed", path = request.requestURI, fieldErrors = fields))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun malformedRequest(ex: Exception, request: HttpServletRequest) =
        response(HttpStatus.BAD_REQUEST, "Request body or parameters are invalid", request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun methodNotAllowed(ex: HttpRequestMethodNotSupportedException, request: HttpServletRequest) =
        response(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint", request)

    @ExceptionHandler(DataAccessException::class)
    fun databaseUnavailable(ex: DataAccessException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("event=user.database.unavailable | msg=Users database operation failed", ex)
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Database is temporarily unavailable", request)
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("event=user.request.failed | msg=Unexpected users service error", ex)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request)
    }

    private fun response(status: HttpStatus, message: String?, request: HttpServletRequest): ResponseEntity<ApiError> {
        val path = request.requestURI.replace(Regex("[\\r\\n]"), "")
        if (status.is4xxClientError) {
            log.warn("event=user.request.rejected | msg=User request rejected | status={} path={}", status.value(), path)
        }
        return ResponseEntity.status(status).body(ApiError(status = status.value(), error = status.reasonPhrase, message = message ?: status.reasonPhrase, path = path))
    }
}
