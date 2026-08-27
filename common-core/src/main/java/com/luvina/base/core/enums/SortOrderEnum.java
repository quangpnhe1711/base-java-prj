package com.luvina.base.core.enums;

import org.springframework.data.domain.Sort;

/**
 * Sort direction accepted by paged search requests.
 */
public enum SortOrderEnum {

    /** Ascending order. */
    ASC(Sort.Direction.ASC),

    /** Descending order. */
    DESC(Sort.Direction.DESC);

    private final Sort.Direction direction;

    SortOrderEnum(Sort.Direction direction) {
        this.direction = direction;
    }

    /**
     * Returns the matching Spring Data direction.
     *
     * @return Spring Data sort direction
     */
    public Sort.Direction direction() {
        return direction;
    }
}
