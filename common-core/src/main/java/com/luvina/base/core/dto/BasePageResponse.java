package com.luvina.base.core.dto;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;

/**
 * Common paging metadata returned by every paged endpoint.
 *
 * <p>Concrete responses extend this class and add the page content under a
 * domain-specific field name.
 */
@Getter
@Setter
public class BasePageResponse {

    /** Total number of matching records across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** One-based index of the current page. */
    private int page;

    /** Number of records per page. */
    private int pageSize;

    /**
     * Copies paging metadata from a Spring Data page, converting the page index
     * back to the one-based form used on the wire.
     *
     * @param source page returned by the repository
     */
    public void applyPageMetadata(Page<?> source) {
        this.totalElements = source.getTotalElements();
        this.totalPages = source.getTotalPages();
        this.page = source.getNumber() + 1;
        this.pageSize = source.getSize();
    }
}
