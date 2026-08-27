package com.luvina.base.template.dto.request;

import com.luvina.base.core.dto.BasePageRequest;
import com.luvina.base.template.enums.SampleStatus;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Filters for the paged sample search.
 *
 * <p>Extends {@link BasePageRequest}, so paging and sorting parameters are
 * declared once for the whole codebase. Every field is optional: an absent
 * filter means "do not restrict on this".
 */
@Getter
@Setter
public class SampleSearchRequest extends BasePageRequest {

    /** Partial, case-insensitive match on the code. */
    @Size(max = 32)
    private String code;

    /** Partial, case-insensitive match on the name. */
    @Size(max = 255)
    private String name;

    /** Exact match on the lifecycle state. */
    private SampleStatus status;
}
