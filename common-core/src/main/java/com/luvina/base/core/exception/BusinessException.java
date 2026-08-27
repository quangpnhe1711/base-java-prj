package com.luvina.base.core.exception;

import org.springframework.http.HttpStatus;

import com.luvina.base.core.i18n.ErrorCode;

import lombok.Getter;

/**
 * Base class for every expected, business-level failure.
 *
 * <p>The message is expected to be already localised, normally produced by
 * {@link com.luvina.base.core.i18n.MessageUtil}. The exception also carries the
 * {@link ErrorCode} and the HTTP status the API should answer with, so
 * {@link GlobalExceptionHandler} needs no per-exception mapping table.
 *
 * <p>Throw this (or a subclass) for anything a caller can act on. Never throw it
 * for programming errors; let those bubble up as-is and be reported as 500.
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Stable error code echoed back to the client. */
    private final transient ErrorCode errorCode;

    /** HTTP status the API should answer with. */
    private final transient HttpStatus status;

    /**
     * Creates a business failure answered with {@code 400 Bad Request}.
     *
     * @param errorCode error code
     * @param message   localised message
     */
    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Creates a business failure with an explicit HTTP status.
     *
     * @param errorCode error code
     * @param message   localised message
     * @param status    HTTP status to answer with
     */
    public BusinessException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }
}
