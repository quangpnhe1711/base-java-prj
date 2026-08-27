package com.luvina.base.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} and friends.
 *
 * <p>Kept in its own class so that it can be switched off together with the rest
 * of authentication. Left always on, a {@code @PreAuthorize} rule would reject
 * the anonymous caller with 403 even in the {@code app.security.enabled: false}
 * mode, which would make that mode useless for anything but read endpoints.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.security", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableMethodSecurity
public class MethodSecurityConfig {
}
