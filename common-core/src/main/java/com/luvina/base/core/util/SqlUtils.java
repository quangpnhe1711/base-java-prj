package com.luvina.base.core.util;

import com.luvina.base.core.constant.AppConstants;

/**
 * Helpers for native queries.
 */
public final class SqlUtils {

    private SqlUtils() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }

    /**
     * Escapes the wildcard characters of a LIKE pattern so that user input is
     * matched literally.
     *
     * <p>This protects the <em>semantics</em> of the query, not against injection;
     * the value must still be bound as a parameter, never concatenated. The query
     * has to declare the escape character explicitly, for example:
     *
     * <pre>
     * WHERE name LIKE CONCAT('%', :name, '%') ESCAPE '\'
     * </pre>
     *
     * @param value raw user input, may be {@code null}
     * @return escaped value, or the input unchanged when null or empty
     */
    public static String escapeLike(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
