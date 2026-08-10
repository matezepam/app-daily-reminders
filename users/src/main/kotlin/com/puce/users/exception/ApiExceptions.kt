package com.puce.users.exception

open class ApiException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String) : ApiException(message)
class UserConflictException(message: String) : ApiException(message)
class UserAuthenticationException(message: String) : ApiException(message)
class IdentityProviderException(message: String) : ApiException(message)
