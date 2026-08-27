package com.luvina.base.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.AuditorAware;

/**
 * Feeds JPA auditing with the id of the authenticated caller, so that
 * {@code createdUserId} and {@code updatedUserId} are filled without any service
 * having to set them.
 *
 * <p>Returning an empty value leaves the column untouched. That happens for
 * unauthenticated flows such as scheduled jobs or migrations, and is preferable
 * to inventing a placeholder id that pollutes the audit trail.
 */
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        return SecurityUtils.getCurrentUserId();
    }
}
