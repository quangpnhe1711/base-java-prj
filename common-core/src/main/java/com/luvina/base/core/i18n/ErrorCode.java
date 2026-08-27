package com.luvina.base.core.i18n;

/**
 * Stable, machine-readable error codes shared by every service.
 *
 * <p>Each constant is also the message key looked up in
 * {@code messages/core-errors.properties}. Codes are never renumbered or reused,
 * because clients and QA scripts match on them.
 *
 * <p>Services that need extra codes declare their own enum and pass it to
 * {@link MessageUtil#getMessage(String, Object[], java.util.Locale)}; they must not
 * add constants here.
 */
public enum ErrorCode {

    /** {0} is required. */
    ERR001,

    /** {0} already exists. */
    ERR002,

    /** {0} does not exist. */
    ERR003,

    /** {0} must not exceed {1} characters. */
    ERR004,

    /** {0} must be at least {1} characters. */
    ERR005,

    /** {0} must be between {1} and {2} characters. */
    ERR006,

    /** {0} has an invalid format. */
    ERR010,

    /** {0} must be less than or equal to {1}. */
    ERR011,

    /** The requested data does not exist. */
    ERR012,

    /** Invalid email address. */
    ERR013,

    /** Some fields are still invalid. */
    ERR014,

    /** The system is temporarily unavailable. */
    ERR015,

    /** At least one search condition is required. */
    ERR016,

    /** {0} is invalid. */
    ERR017,

    /** The uploaded file exceeds the size limit of {0}. */
    ERR023,

    /** Unsupported file format; allowed formats are {0}. */
    ERR026,

    /** The combination of {0} and {1} already exists. */
    ERR027,

    /** {0} was modified by someone else; reload and retry. */
    ERR029,

    /** The caller is authenticated but not allowed to perform the action. */
    ERR040,

    /** The caller is not authenticated. */
    ERR041,

    /** {0} is still referenced by other data and cannot be deleted. */
    ERR042
}
