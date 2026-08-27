package com.luvina.base.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.luvina.base.core.constant.AppConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * Read-only access to the authenticated caller.
 *
 * <p>Values come from the {@link Jwt} that Spring Security has already
 * <strong>verified</strong>: signature, issuer, audience and expiry were all
 * checked before the request reached a controller.
 *
 * <p>Never parse the {@code Authorization} header by hand to read claims. An
 * unverified token is attacker-controlled input, and using its {@code sub} for
 * audit columns lets anyone write rows under any identity.
 */
@Slf4j
public final class SecurityUtils {

    private static final String CLAIM_PREFERRED_USERNAME = "preferred_username";
    private static final String CLAIM_EMAIL = "email";

    private SecurityUtils() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }

    /**
     * Returns the verified token of the current request.
     *
     * @return the token, or empty for an unauthenticated request
     */
    public static Optional<Jwt> getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return Optional.of(jwtAuthentication.getToken());
        }
        return Optional.empty();
    }

    /**
     * Returns the caller id, taken from the {@code sub} claim.
     *
     * @return the user id, or empty when unauthenticated or when {@code sub} is
     *         not a UUID, as is the case for some service accounts
     */
    public static Optional<UUID> getCurrentUserId() {
        return getCurrentJwt()
                .map(Jwt::getSubject)
                .flatMap(SecurityUtils::parseUuid);
    }

    /**
     * Returns the caller login name.
     *
     * @return the username, or empty when unauthenticated
     */
    public static Optional<String> getCurrentUsername() {
        return getClaim(CLAIM_PREFERRED_USERNAME);
    }

    /**
     * Returns the caller email address.
     *
     * @return the email, or empty when unauthenticated or when the token carries
     *         no email claim
     */
    public static Optional<String> getCurrentEmail() {
        return getClaim(CLAIM_EMAIL);
    }

    /**
     * Returns the raw token value, for propagating the caller identity to a
     * downstream service.
     *
     * @return the encoded token, or empty when unauthenticated
     */
    public static Optional<String> getCurrentTokenValue() {
        return getCurrentJwt().map(Jwt::getTokenValue);
    }

    /**
     * Reads an arbitrary string claim from the verified token.
     *
     * @param claimName name of the claim
     * @return claim value, or empty when unauthenticated or when the claim is absent
     */
    public static Optional<String> getClaim(String claimName) {
        return getCurrentJwt().map(jwt -> jwt.getClaimAsString(claimName));
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException ex) {
            log.debug("Token subject is not a UUID, skipping user id resolution");
            return Optional.empty();
        }
    }
}
