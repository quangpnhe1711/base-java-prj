package com.luvina.base.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luvina.base.core.dto.ApiError;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Writes the shared {@link ApiError} body for failures raised inside the filter
 * chain, before any controller runs.
 *
 * <p>Without this, a missing or expired token produces an empty 401 body, which
 * breaks the promise that every failure has the same shape. That is the single
 * most common failure a front end has to handle, so it is worth wiring.
 *
 * <p>Failures raised after dispatch, such as a rejected {@code @PreAuthorize},
 * are handled by {@link SecurityExceptionHandler} instead.
 */
@RequiredArgsConstructor
public class ApiErrorSecurityEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final MessageUtil messageUtil;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.ERR041);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, ErrorCode.ERR040);
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, ErrorCode code) throws IOException {
        ApiError body = ApiError.builder()
                .timestamp(OffsetDateTime.now())
                .status(status.value())
                .code(code.name())
                .message(messageUtil.getMessage(code))
                .path(request.getRequestURI())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
