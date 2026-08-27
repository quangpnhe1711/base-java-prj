package com.luvina.base.template.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.luvina.base.template.enums.SampleStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * A sample as returned by the API.
 *
 * <p>Entities are never serialised directly. A response DTO keeps the wire
 * contract stable when the schema changes, and keeps audit or internal columns
 * out of the payload.
 *
 * <p>{@code updatedAt} is included deliberately: the client must send it back on
 * update and delete for the optimistic-lock check.
 */
@Getter
@Setter
public class SampleResponse {

    /** Record id. */
    private UUID id;

    /** Business identifier. */
    private String code;

    /** Display name. */
    private String name;

    /** Free-text description. */
    private String description;

    /** Lifecycle state. */
    private SampleStatus status;

    /** Date the record takes effect. */
    private LocalDate effectiveDate;

    /** Optimistic-lock token to send back on the next write. */
    private OffsetDateTime updatedAt;
}
