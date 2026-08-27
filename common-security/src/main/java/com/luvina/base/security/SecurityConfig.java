package com.luvina.base.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The one and only filter chain of a service.
 *
 * <p>Adding {@code common-security} to a service secures it: every endpoint
 * requires a valid bearer token unless its path is listed in
 * {@code app.security.public-paths}. A service must not declare a second
 * {@link SecurityFilterChain}; with two unordered chains the effective policy is
 * whichever one Spring happens to register first, which is how an API ends up
 * silently open.
 *
 * <p>Method-level security lives in {@link MethodSecurityConfig}, so that
 * authorisation rules such as {@code @PreAuthorize("hasRole('ADMIN')")} switch
 * off together with authentication rather than rejecting every anonymous caller.
 *
 * <p>The {@code JwtDecoder} is not declared here. Spring Boot builds a fully
 * validating one from
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}: it discovers the
 * JWKS endpoint, caches the keys and checks the {@code iss} claim. Pointing a
 * decoder straight at the issuer URI as if it were the JWKS URI is a common
 * mistake and skips issuer validation.
 *
 * <p>An identity provider is optional for local work. With
 * {@code app.security.enabled: false} the chain permits every request and no
 * decoder is built, so the service starts with nothing but a database. That mode
 * warns on every startup and must never reach a deployed environment.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityProperties properties;
    private final ApiErrorSecurityEntryPoint securityEntryPoint;

    /**
     * Builds the stateless resource-server filter chain.
     *
     * @param http security builder
     * @return the configured filter chain
     * @throws Exception when the chain cannot be built
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // No session, no login form: the bearer token is the whole identity,
            // which also makes CSRF protection irrelevant here.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        applyCors(http);

        if (!properties.isEnabled()) {
            log.warn("app.security.enabled=false - every endpoint is OPEN and no token is "
                    + "verified. Intended for local development only.");
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http
            .authorizeHttpRequests(auth -> auth
                // Preflight carries no credentials and must never be challenged.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(properties.getPublicPaths().toArray(String[]::new)).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(securityEntryPoint)
                .accessDeniedHandler(securityEntryPoint))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(
                        new JwtRoleConverter(properties.getResourceClientId()))));

        return http.build();
    }

    /**
     * Enables CORS only when an origin is configured, so an unconfigured
     * environment fails closed rather than open.
     */
    private void applyCors(HttpSecurity http) throws Exception {
        if (properties.getCors().isEnabled()) {
            http.cors(Customizer.withDefaults());
        } else {
            http.cors(cors -> cors.disable());
        }
    }

    /**
     * Builds the CORS policy from configuration. Only registered when at least
     * one origin is configured, so an unconfigured environment stays closed.
     *
     * @return the CORS source applied to every path
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        SecurityProperties.Cors cors = properties.getCors();
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(cors.getAllowedOrigins());
        configuration.setAllowedMethods(cors.getAllowedMethods());
        configuration.setAllowedHeaders(cors.getAllowedHeaders());
        configuration.setExposedHeaders(cors.getExposedHeaders());
        configuration.setAllowCredentials(cors.isAllowCredentials());
        configuration.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
