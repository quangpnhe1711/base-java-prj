package com.luvina.base.core.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/**
 * The single error shape returned by every failing endpoint.
 *
 * <p>Having exactly one error contract means clients never have to branch on the
 * status code to know how to parse a failure.
 *
 * <pre>
 * {
 *   "timestamp": "2026-01-01T10:00:00+07:00",
 *   "status": 400,
 *   "code": "ERR001",
 *   "message": "Name is required.",
 *   "path": "/api/v1/samples",
 *   "fieldErrors": [ { "field": "name", "message": "must not be blank" } ]
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Moment the error was produced. */
    private final OffsetDateTime timestamp;

    /** HTTP status code, repeated in the body for convenience. */
    private final int status;

    /** Stable machine-readable error code, for example {@code ERR001}. */
    private final String code;

    /** Human-readable, already localised message. */
    private final String message;

    /** Request path that produced the error. */
    private final String path;

    /** Per-field validation failures. Absent for non-validation errors. */
    private final List<FieldErrorDetail> fieldErrors;

    /**
     * One rejected field of a validated request body.
     *
     * @param field   name of the rejected field
     * @param message reason the value was rejected
     */
    public record FieldErrorDetail(String field, String message) { }
}
