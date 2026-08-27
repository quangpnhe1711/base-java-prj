package com.luvina.base.template.dto.request;

import java.time.LocalDate;

import com.luvina.base.template.enums.SampleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for creating a sample.
 *
 * <p>Structural rules live here as Bean Validation annotations, so they are
 * enforced before any service code runs. Rules that need a database lookup, such
 * as uniqueness, belong in the service instead.
 */
@Getter
@Setter
public class SampleCreateRequest {

    /** Business identifier. Uppercase letters, digits and underscore. */
    @NotBlank
    @Size(max = 32)
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "code accepts uppercase letters, digits and underscore only")
    private String code;

    /** Display name. */
    @NotBlank
    @Size(max = 255)
    private String name;

    /** Optional free-text description. */
    @Size(max = 1000)
    private String description;

    /** Lifecycle state. */
    @NotNull
    private SampleStatus status = SampleStatus.ACTIVE;

    /** Date the record takes effect. */
    private LocalDate effectiveDate;
}
