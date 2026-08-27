package com.luvina.base.template.dto.request;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.luvina.base.template.enums.SampleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for updating a sample.
 *
 * <p>{@code code} is absent on purpose: a business identifier that other records
 * may already reference is not editable through a normal update.
 *
 * <p>{@code updatedAt} is the optimistic-lock token the client received when it
 * read the record. Without it the request cannot tell a fresh edit from one made
 * against a stale copy.
 */
@Getter
@Setter
public class SampleUpdateRequest {

    /** Display name. */
    @NotBlank
    @Size(max = 255)
    private String name;

    /** Optional free-text description. */
    @Size(max = 1000)
    private String description;

    /** Lifecycle state. */
    @NotNull
    private SampleStatus status;

    /** Date the record takes effect. */
    private LocalDate effectiveDate;

    /** Value of {@code updatedAt} the client last saw for this record. */
    @NotNull
    private OffsetDateTime updatedAt;
}
