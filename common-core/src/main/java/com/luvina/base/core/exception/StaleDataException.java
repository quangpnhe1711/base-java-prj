package com.luvina.base.core.exception;

import org.springframework.http.HttpStatus;

import com.luvina.base.core.i18n.ErrorCode;

/**
 * Thrown when the {@code updatedAt} token supplied by the client no longer
 * matches the stored row, meaning somebody else changed it in the meantime.
 * Answered with {@code 409 Conflict}.
 *
 * @see com.luvina.base.core.locking.OptimisticLockSupport
 */
public class StaleDataException extends BusinessException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception with the default {@link ErrorCode#ERR029}.
     *
     * @param message localised message
     */
    public StaleDataException(String message) {
        super(ErrorCode.ERR029, message, HttpStatus.CONFLICT);
    }
}
