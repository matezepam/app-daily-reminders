package com.puce.reminder.exception

import com.puce.reminder.dto.ApiError
import com.puce.reminder.service.CourseService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DataAccessException
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class RestExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, request: HttpServletRequest) = response(HttpStatus.NOT_FOUND, ex.message, request)

    @ExceptionHandler(ForbiddenException::class)
    fun forbidden(ex: ForbiddenException, request: HttpServletRequest) = response(HttpStatus.FORBIDDEN, ex.message, request)

    @ExceptionHandler(AccessDeniedException::class)
    fun accessDenied(ex: AccessDeniedException, request: HttpServletRequest) =
        response(HttpStatus.FORBIDDEN, "You do not have permission to perform this operation", request)

    @ExceptionHandler(ConflictException::class)
    fun conflict(ex: ConflictException, request: HttpServletRequest) = response(HttpStatus.CONFLICT, ex.message, request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun dataConflict(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val duplicateCourse = generateSequence<Throwable>(ex) { it.cause }
            .mapNotNull { it.message }
            .any { it.contains("uk_courses_professor_name_description", ignoreCase = true) }
        val message = if (duplicateCourse) {
            CourseService.DUPLICATE_COURSE_MESSAGE
        } else {
            "The requested data conflicts with an existing record"
        }
        return response(HttpStatus.CONFLICT, message, request)
    }

    @ExceptionHandler(BadRequestException::class)
    fun badRequest(ex: BadRequestException, request: HttpServletRequest) = response(HttpStatus.BAD_REQUEST, ex.message, request)

    @ExceptionHandler(BadGatewayException::class)
    fun badGateway(ex: BadGatewayException, request: HttpServletRequest) = response(HttpStatus.BAD_GATEWAY, ex.message, request)

    @ExceptionHandler(ServiceUnavailableException::class)
    fun serviceUnavailable(ex: ServiceUnavailableException, request: HttpServletRequest) = response(HttpStatus.SERVICE_UNAVAILABLE, ex.message, request)

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
        log.error("event=database.unavailable | msg=Academic reminder database operation failed", ex)
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Database is temporarily unavailable", request)
    }

    @ExceptionHandler(Exception::class)
    fun unexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("event=request.failed | msg=Unexpected academic reminder error", ex)
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request)
    }

    private fun response(status: HttpStatus, message: String?, request: HttpServletRequest): ResponseEntity<ApiError> {
        val path = request.requestURI.replace(Regex("[\\r\\n]"), "")
        if (status.is4xxClientError) {
            log.warn("event=request.rejected | msg=Request rejected | status={} path={}", status.value(), path)
        }
        return ResponseEntity.status(status).body(ApiError(status = status.value(), error = status.reasonPhrase, message = message ?: status.reasonPhrase, path = path))
    }
}
