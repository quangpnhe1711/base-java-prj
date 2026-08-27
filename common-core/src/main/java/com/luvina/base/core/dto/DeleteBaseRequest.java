package com.luvina.base.core.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Payload for deleting a single record.
 *
 * <p>Sent as a request body rather than a path variable because it also carries
 * {@code updatedAt}, the optimistic-lock token the client received when it read
 * the record. See {@link com.luvina.base.core.locking.OptimisticLockSupport}.
 */
@Getter
@Setter
public class DeleteBaseRequest {

    /** Id of the record to delete. */
    @NotNull
    private UUID id;

    /** Value of {@code updatedAt} the client last saw for this record. */
    @NotNull
    private OffsetDateTime updatedAt;
}
