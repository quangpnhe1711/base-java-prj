package com.luvina.base.core.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.luvina.base.core.entity.BaseEntity;

/**
 * Base repository for soft-deletable entities.
 *
 * <p>Every entity repository should extend this instead of {@code JpaRepository}
 * directly, so that soft-delete filtering stays a repository-level concern.
 *
 * @param <T>  entity type
 * @param <I>  primary key type, normally {@code UUID}
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, I> extends JpaRepository<T, I> {

    /**
     * Finds a non-deleted entity by its id.
     *
     * @param id primary key
     * @return the entity, or empty when missing or soft-deleted
     */
    Optional<T> findByIdAndDeleteFlagFalse(I id);

    /**
     * Lists all non-deleted entities.
     *
     * @param sort sort order to apply
     * @return non-deleted entities
     */
    List<T> findAllByDeleteFlagFalse(Sort sort);

    /**
     * Tells whether a non-deleted entity with the given id exists.
     *
     * @param id primary key
     * @return {@code true} when the row exists and is not soft-deleted
     */
    boolean existsByIdAndDeleteFlagFalse(I id);
}
