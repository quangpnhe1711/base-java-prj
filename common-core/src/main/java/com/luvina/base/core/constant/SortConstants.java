package com.luvina.base.core.constant;

/**
 * Paging and sorting defaults.
 */
public final class SortConstants {

    /** Default number of records per page. */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** Default one-based page index. */
    public static final int DEFAULT_PAGE = 1;

    private SortConstants() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }
}
