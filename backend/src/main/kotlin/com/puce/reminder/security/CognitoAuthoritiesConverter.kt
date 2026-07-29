package com.puce.reminder.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class CognitoAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {
    override fun convert(source: Jwt): Collection<GrantedAuthority> =
        source.getClaimAsStringList("cognito:groups")
            .orEmpty()
            .mapNotNull(CognitoRole::fromGroup)
            .distinct()
            .map { role -> SimpleGrantedAuthority("ROLE_${role.name}") }
}
