package com.luvina.base.template.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.luvina.base.core.repository.BaseRepository;
import com.luvina.base.template.entity.Sample;
import com.luvina.base.template.projection.SampleListProjection;
import com.luvina.base.template.repository.sql.SampleSql;

/**
 * Persistence for {@link Sample}.
 *
 * <p>Extends {@link BaseRepository}, which supplies the soft-delete-aware
 * lookups. Derived query methods cover the simple cases; the paged search uses a
 * native statement returning a projection.
 *
 * <p>Every method name here ends in {@code DeleteFlagFalse}. A query that
 * forgets it will happily return logically deleted rows.
 */
@Repository
public interface SampleRepository extends BaseRepository<Sample, UUID> {

    /**
     * Tells whether a code is already taken.
     *
     * @param code business identifier
     * @return true when a non-deleted row already uses the code
     */
    boolean existsByCodeAndDeleteFlagFalse(String code);

    /**
     * Tells whether a code is taken by a row other than the given one, used when
     * updating.
     *
     * @param code business identifier
     * @param id   id to exclude from the check
     * @return true when another non-deleted row already uses the code
     */
    boolean existsByCodeAndIdNotAndDeleteFlagFalse(String code, UUID id);

    /**
     * Runs the paged search.
     *
     * @param code     partial code filter, may be null
     * @param name     partial name filter, may be null
     * @param status   exact status filter, may be null
     * @param pageable paging and sorting
     * @return one page of matching rows
     */
    @Query(value = SampleSql.SEARCH, countQuery = SampleSql.SEARCH_COUNT, nativeQuery = true)
    Page<SampleListProjection> search(@Param("code") String code,
                                      @Param("name") String name,
                                      @Param("status") String status,
                                      Pageable pageable);
}
