package com.luvina.base.core.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.luvina.base.core.constant.AppConstants;

/**
 * Conversions between the temporal types used across the layers.
 *
 * <p>Native queries and projections hand back {@link Instant}; entities and DTOs
 * use {@link OffsetDateTime}. These helpers bridge the two without every service
 * repeating the conversion.
 *
 * <p>The no-zone overloads use the JVM default zone, which deployments pin with
 * {@code TZ} or {@code spring.jackson.time-zone}. Do not hardcode an offset here.
 */
public final class DateTimeUtil {

    private DateTimeUtil() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }

    /**
     * Converts an instant to an offset date-time in the JVM default zone.
     *
     * @param instant value to convert, may be {@code null}
     * @return converted value, or {@code null} when the input was {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant) {
        return toOffsetDateTime(instant, ZoneId.systemDefault());
    }

    /**
     * Converts an instant to an offset date-time in an explicit zone.
     *
     * @param instant value to convert, may be {@code null}
     * @param zone    target zone
     * @return converted value, or {@code null} when the input was {@code null}
     */
    public static OffsetDateTime toOffsetDateTime(Instant instant, ZoneId zone) {
        return instant == null ? null : instant.atZone(zone).toOffsetDateTime();
    }

    /**
     * Converts an offset date-time to an instant.
     *
     * @param value value to convert, may be {@code null}
     * @return converted value, or {@code null} when the input was {@code null}
     */
    public static Instant toInstant(OffsetDateTime value) {
        return value == null ? null : value.toInstant();
    }
}
