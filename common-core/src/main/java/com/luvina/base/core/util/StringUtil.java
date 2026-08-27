package com.luvina.base.core.util;

import com.luvina.base.core.constant.AppConstants;

/**
 * Small string helpers that Apache Commons does not cover in the exact shape we
 * want. Keep this class tiny: if Commons Lang or the JDK already does it, use
 * that instead.
 */
public final class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }

    /**
     * Trims a value and collapses blank input to {@code null}, so that an empty
     * search box and an absent parameter reach the repository the same way.
     *
     * @param value value to normalise, may be {@code null}
     * @return trimmed value, or {@code null} when it was null or blank
     */
    public static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
