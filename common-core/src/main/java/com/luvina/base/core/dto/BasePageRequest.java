package com.luvina.base.core.dto;

import com.luvina.base.core.constant.SortConstants;
import com.luvina.base.core.enums.SortOrderEnum;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * Common paging and sorting parameters.
 *
 * <p>Search requests extend this class and add their own filter fields.
 * {@code page} is <strong>one-based</strong> on the wire; use
 * {@link #toPageable(String)} to convert it to a Spring {@code Pageable}.
 */
@Getter
@Setter
public class BasePageRequest {

    /** One-based page index. Defaults to the first page. */
    @Min(value = 1, message = "page must be greater than or equal to 1")
    private Integer page = SortConstants.DEFAULT_PAGE;

    /** Number of records per page. */
    @Min(value = 1, message = "pageSize must be greater than or equal to 1")
    private Integer pageSize = SortConstants.DEFAULT_PAGE_SIZE;

    /** Name of the field to sort by. Interpretation is left to the service. */
    private String orderBy;

    /** Sort direction. Defaults to ascending when absent. */
    private SortOrderEnum orderType = SortOrderEnum.ASC;

    /**
     * Returns the zero-based page index expected by Spring Data.
     *
     * @return zero-based page index
     */
    public int zeroBasedPage() {
        return (page == null ? SortConstants.DEFAULT_PAGE : page) - 1;
    }

    /**
     * Returns the effective page size, falling back to the default.
     *
     * @return page size
     */
    public int effectivePageSize() {
        return pageSize == null ? SortConstants.DEFAULT_PAGE_SIZE : pageSize;
    }

    /**
     * Returns the sort direction keyword to inline into a native query.
     *
     * @return {@code "ASC"} or {@code "DESC"}
     */
    public String sortDirection() {
        return orderType == null ? SortOrderEnum.ASC.name() : orderType.name();
    }
}
