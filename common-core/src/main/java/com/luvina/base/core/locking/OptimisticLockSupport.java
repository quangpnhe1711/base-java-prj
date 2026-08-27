package com.luvina.base.core.locking;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.luvina.base.core.entity.BaseEntity;
import com.luvina.base.core.exception.ResourceNotFoundException;
import com.luvina.base.core.exception.StaleDataException;
import com.luvina.base.core.i18n.ErrorCode;
import com.luvina.base.core.i18n.MessageKey;
import com.luvina.base.core.i18n.MessageUtil;
import com.luvina.base.core.repository.BaseRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lost-update protection built on the {@code updatedAt} column.
 *
 * <p>The client receives {@code updatedAt} when it reads a record and sends the
 * same value back on update or delete. If the stored value has moved on, someone
 * else changed the row and the request is rejected with
 * {@link StaleDataException} rather than silently overwriting their work.
 *
 * <p>This is deliberately not JPA {@code @Version}: the token is part of the
 * public API contract, and it costs no extra column. The trade-off is that the
 * check happens on read, so it must run inside the same transaction as the write.
 *
 * <p>Services call {@link #loadForUpdate} to get the managed entity, then apply
 * their own field mapping. There is no generic copy-all helper on purpose:
 * blanket property copying is how fields get overwritten by accident.
 */
@Component
@RequiredArgsConstructor
public class OptimisticLockSupport {

    /**
     * Timestamps are compared at microsecond precision, the resolution PostgreSQL
     * stores. Comparing raw nanoseconds would reject every valid request.
     */
    private static final ChronoUnit COMPARISON_PRECISION = ChronoUnit.MICROS;

    private final MessageUtil messageUtil;

    /**
     * Loads a non-deleted entity and verifies that the caller is working from the
     * current version of it.
     *
     * @param repository        repository owning the entity
     * @param id                primary key
     * @param expectedUpdatedAt {@code updatedAt} value the client last saw
     * @param entityLabel       label used to build the error message
     * @param <T>               entity type
     * @param <I>               primary key type
     * @return the managed entity, safe to modify
     * @throws ResourceNotFoundException when the row is missing or soft-deleted
     * @throws StaleDataException        when the row changed since the client read it
     */
    public <T extends BaseEntity, I> T loadForUpdate(BaseRepository<T, I> repository,
                                                     I id,
                                                     OffsetDateTime expectedUpdatedAt,
                                                     MessageKey entityLabel) {
        T entity = repository.findByIdAndDeleteFlagFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageUtil.format(ErrorCode.ERR003, entityLabel)));

        if (!sameVersion(entity.getUpdatedAt(), expectedUpdatedAt)) {
            throw new StaleDataException(messageUtil.format(ErrorCode.ERR029, entityLabel));
        }
        return entity;
    }

    /**
     * Loads an entity, verifies its version and marks it soft-deleted.
     *
     * <p>{@code updatedUserId} is left to JPA auditing, which fills it from the
     * authenticated principal.
     *
     * @param repository        repository owning the entity
     * @param id                primary key
     * @param expectedUpdatedAt {@code updatedAt} value the client last saw
     * @param entityLabel       label used to build the error message
     * @param <T>               entity type
     * @param <I>               primary key type
     * @return the soft-deleted entity
     */
    public <T extends BaseEntity, I> T softDelete(BaseRepository<T, I> repository,
                                                  I id,
                                                  OffsetDateTime expectedUpdatedAt,
                                                  MessageKey entityLabel) {
        T entity = loadForUpdate(repository, id, expectedUpdatedAt, entityLabel);
        entity.setDeleteFlag(Boolean.TRUE);
        return entity;
    }

    private boolean sameVersion(OffsetDateTime stored, OffsetDateTime expected) {
        if (stored == null || expected == null) {
            return false;
        }
        return stored.toInstant().truncatedTo(COMPARISON_PRECISION)
                .equals(expected.toInstant().truncatedTo(COMPARISON_PRECISION));
    }
}
