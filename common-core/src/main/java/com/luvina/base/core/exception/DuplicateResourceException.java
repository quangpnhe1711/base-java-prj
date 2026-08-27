package com.luvina.base.core.exception;

import org.springframework.http.HttpStatus;

import com.luvina.base.core.i18n.ErrorCode;

/**
 * Thrown when creating or updating a record would break a uniqueness rule.
 * Answered with {@code 409 Conflict}.
 */
public class DuplicateResourceException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the default {@link ErrorCode#ERR002}.
     *
     * @param message localised message
     */
    public DuplicateResourceException(String message) {
        super(ErrorCode.ERR002, message, HttpStatus.CONFLICT);
    }

    /**
     * Creates the exception with an explicit duplication code, for example
     * {@link ErrorCode#ERR027} for a composite key.
     *
     * @param errorCode error code
     * @param message   localised message
     */
    public DuplicateResourceException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
