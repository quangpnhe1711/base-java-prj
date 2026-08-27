package com.luvina.base.security;

import java.time.OffsetDateTime;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.luvina.base.core.dto.ApiError;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reports authorisation failures using the shared {@link ApiError} contract.
 *
 * <p>Kept out of {@code common-core} so that module does not depend on Spring
 * Security. Only failures raised <em>after</em> the request reaches a controller
 * arrive here, chiefly {@code @PreAuthorize} rejections; a missing or invalid
 * token is rejected earlier by the filter chain.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@RequiredArgsConstructor
public class SecurityExceptionHandler {

    private final MessageUtil messageUtil;

    /**
     * Handles a caller that is authenticated but lacks the required role.
     *
     * @param ex      the failure
     * @param request current request
     * @return error response with status 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        log.debug("Access denied on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ErrorCode.ERR040, request);
    }

    /**
     * Handles a caller whose credentials could not be established.
     *
     * @param ex      the failure
     * @param request current request
     * @return error response with status 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        log.debug("Authentication failed on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.ERR041, request);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code,
                                          HttpServletRequest request) {
        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .code(code.name())
                .message(messageUtil.getMessage(code))
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
