package com.puce.reminder.exception

open class ApiException(message: String) : RuntimeException(message)
class NotFoundException(message: String) : ApiException(message)
class ForbiddenException(message: String) : ApiException(message)
class ConflictException(message: String) : ApiException(message)
class BadRequestException(message: String) : ApiException(message)
class BadGatewayException(message: String) : ApiException(message)
class ServiceUnavailableException(message: String) : ApiException(message)
