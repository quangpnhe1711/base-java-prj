package com.luvina.base.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Everything about the security setup that varies between services and
 * environments, so that no service has to redeclare a filter chain.
 *
 * <p>The token issuer itself is <em>not</em> configured here. It uses the
 * standard Spring property
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}, which lets Spring
 * Boot build a fully validating {@code JwtDecoder} on its own.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /**
     * Whether token authentication is enforced.
     *
     * <p>Set to {@code false} to run without an identity provider, which is what
     * lets a developer start the service with nothing but a database. The filter
     * chain then permits every request and logs a warning on every startup, so an
     * unsecured service cannot go unnoticed.
     *
     * <p>Must be {@code true} in every deployed environment.
     */
    private boolean enabled = true;

    /**
     * Ant patterns reachable without a token. Health and API documentation are
     * open by default; anything else must be listed explicitly.
     */
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**"));

    /**
     * Keycloak client id whose {@code resource_access} roles should be granted in
     * addition to the realm roles. Leave empty to use realm roles only.
     */
    private String resourceClientId = "";

    /** Cross-origin settings for browsers calling this service directly. */
    private Cors cors = new Cors();

    /**
     * CORS configuration. Disabled until at least one origin is listed, so a
     * misconfigured environment fails closed rather than open.
     */
    @Getter
    @Setter
    public static class Cors {

        /**
         * Allowed origin patterns, for example {@code https://app.example.com}.
         * Never use {@code *} together with credentials.
         */
        private List<String> allowedOrigins = new ArrayList<>();

        /** Allowed HTTP methods. */
        private List<String> allowedMethods =
                new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        /** Allowed request headers. */
        private List<String> allowedHeaders =
                new ArrayList<>(List.of("Authorization", "Content-Type", "Accept-Language"));

        /** Response headers exposed to the browser. */
        private List<String> exposedHeaders =
                new ArrayList<>(List.of("Content-Disposition"));

        /** Whether the browser may send credentials. */
        private boolean allowCredentials = true;

        /** How long a preflight response may be cached, in seconds. */
        private long maxAge = 3600L;

        /**
         * Tells whether CORS should be wired at all.
         *
         * @return true when at least one origin is configured
         */
        public boolean isEnabled() {
            return !allowedOrigins.isEmpty();
        }
    }
}
