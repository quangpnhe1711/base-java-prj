package com.luvina.base.core.exception;

import org.springframework.http.HttpStatus;

import com.luvina.base.core.i18n.ErrorCode;

/**
 * Thrown when a referenced record does not exist or is soft-deleted.
 * Answered with {@code 404 Not Found}.
 */
public class ResourceNotFoundException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the default {@link ErrorCode#ERR003}.
     *
     * @param message localised message
     */
    public ResourceNotFoundException(String message) {
        super(ErrorCode.ERR003, message, HttpStatus.NOT_FOUND);
    }
}
