package com.luvina.base.security;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import lombok.RequiredArgsConstructor;

/**
 * Maps a Keycloak access token onto Spring Security authorities.
 *
 * <p>Keycloak does not put roles in the {@code scope} claim that Spring reads by
 * default. They live in {@code realm_access.roles} and, per client, in
 * {@code resource_access.<clientId>.roles}. Both are translated to
 * {@code ROLE_<NAME>} so that {@code @PreAuthorize("hasRole('ADMIN')")} works as
 * written.
 *
 * <p>Scopes are still converted, as {@code SCOPE_<scope>}, so that both styles of
 * authorisation check remain available.
 */
@RequiredArgsConstructor
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROLES_FIELD = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

    /** Client id whose {@code resource_access} roles are granted; may be blank. */
    private final String resourceClientId;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeConverter.convert(jwt));
        authorities.addAll(toAuthorities(realmRoles(jwt)));
        authorities.addAll(toAuthorities(clientRoles(jwt)));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<String> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptyList();
        }
        Object roles = realmAccess.get(ROLES_FIELD);
        return roles instanceof Collection ? (Collection<String>) roles : Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private Collection<String> clientRoles(Jwt jwt) {
        if (resourceClientId == null || resourceClientId.isBlank()) {
            return Collections.emptyList();
        }
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
        if (resourceAccess == null) {
            return Collections.emptyList();
        }
        Object client = resourceAccess.get(resourceClientId);
        if (!(client instanceof Map)) {
            return Collections.emptyList();
        }
        Object roles = ((Map<String, Object>) client).get(ROLES_FIELD);
        return roles instanceof Collection ? (Collection<String>) roles : Collections.emptyList();
    }

    private List<GrantedAuthority> toAuthorities(Collection<String> roles) {
        return roles.stream()
                .map(role -> ROLE_PREFIX + role.toUpperCase(Locale.ROOT))
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name))
                .toList();
    }
}
