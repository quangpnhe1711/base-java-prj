package com.luvina.base.security;

import java.util.UUID;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luvina.base.core.i18n.MessageUtil;

/**
 * Registers the security infrastructure as soon as this module is on the
 * classpath: the filter chain, role mapping, the auditing source and the JSON
 * error writer.
 *
 * <p>Beans are conditional on being absent, so a service that needs something
 * different simply declares its own.
 */
@AutoConfiguration
@Import({SecurityConfig.class, MethodSecurityConfig.class, SecurityExceptionHandler.class})
public class SecurityAutoConfiguration {

    /**
     * Supplies the current user id to JPA auditing.
     *
     * @return auditor source backed by the verified token
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<UUID> auditorAware() {
        return new AuditorAwareImpl();
    }

    /**
     * Supplies the JSON writer used for filter-level authentication and
     * authorisation failures.
     *
     * @param objectMapper application object mapper
     * @param messageUtil  message resolver
     * @return entry point and access denied handler
     */
    @Bean
    @ConditionalOnMissingBean(ApiErrorSecurityEntryPoint.class)
    public ApiErrorSecurityEntryPoint apiErrorSecurityEntryPoint(ObjectMapper objectMapper,
                                                                 MessageUtil messageUtil) {
        return new ApiErrorSecurityEntryPoint(objectMapper, messageUtil);
    }
}
