package com.luvina.base.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import jakarta.persistence.EntityManager;

/**
 * Turns on JPA auditing so that the audit columns of
 * {@link com.luvina.base.core.entity.BaseEntity} are populated automatically.
 *
 * <p>Timestamps are filled by Hibernate. User ids are filled from the
 * {@code AuditorAware} bean contributed by {@code common-security}; without that
 * module on the classpath they simply stay null.
 */
@Configuration
@ConditionalOnClass(EntityManager.class)
@EnableJpaAuditing
public class JpaAuditConfig {
}
