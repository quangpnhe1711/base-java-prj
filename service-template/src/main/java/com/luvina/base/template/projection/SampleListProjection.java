package com.luvina.base.template.projection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read model of the paged search.
 *
 * <p>A projection interface lets a native query return exactly the columns the
 * screen needs, without loading entities or their associations. Spring Data
 * matches getters to the aliases in the SQL, so the aliases must stay in sync
 * with these names.
 *
 * <p>Timestamps arrive as {@link Instant} from a native query, not as
 * {@code OffsetDateTime}; the mapper converts them.
 */
public interface SampleListProjection {

    /**
     * Returns the record id.
     *
     * @return id
     */
    UUID getId();

    /**
     * Returns the business identifier.
     *
     * @return code
     */
    String getCode();

    /**
     * Returns the display name.
     *
     * @return name
     */
    String getName();

    /**
     * Returns the lifecycle state as stored.
     *
     * @return status name
     */
    String getStatus();

    /**
     * Returns the effective date.
     *
     * @return effective date
     */
    LocalDate getEffectiveDate();

    /**
     * Returns the optimistic-lock token.
     *
     * @return last update timestamp
     */
    Instant getUpdatedAt();
}
