package com.luvina.base.template.repository.sql;

import com.luvina.base.core.constant.AppConstants;

/**
 * Native SQL of the sample repository.
 *
 * <p>Non-trivial SQL lives in a constant class rather than inside a
 * {@code @Query} annotation: text blocks keep the statement readable and
 * reviewable, and the repository interface stays a list of signatures.
 *
 * <p>Two rules apply to every statement here:
 * <ul>
 *   <li>values are bound as named parameters, never concatenated;</li>
 *   <li>column aliases match the getters of the projection interface exactly.</li>
 * </ul>
 */
public final class SampleSql {

    /**
     * Paged search.
     *
     * <p>Each filter is written as {@code (:param IS NULL OR ...)} so one
     * statement serves every combination of filters.
     *
     * <p>Sorting is applied through the {@code Pageable}, not by interpolating a
     * column name into the statement, which would be an injection point.
     *
     * <p>The LIKE patterns declare an escape character, which is what makes
     * {@code SqlUtils.escapeLike} on the parameter meaningful.
     */
    public static final String SEARCH = """
        SELECT
              s.id             AS id
            , s.code           AS code
            , s.name           AS name
            , s.status         AS status
            , s.effective_date AS effectiveDate
            , s.updated_at     AS updatedAt
        FROM t_samples s
        WHERE s.delete_flag = FALSE
          AND (CAST(:code AS TEXT) IS NULL
               OR LOWER(s.code) LIKE LOWER(CONCAT('%', CAST(:code AS TEXT), '%')) ESCAPE '\\')
          AND (CAST(:name AS TEXT) IS NULL
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS TEXT), '%')) ESCAPE '\\')
          AND (CAST(:status AS TEXT) IS NULL OR s.status = CAST(:status AS TEXT))
        """;

    /** Row count matching {@link #SEARCH}, required by Spring Data for paging. */
    public static final String SEARCH_COUNT = """
        SELECT COUNT(1)
        FROM t_samples s
        WHERE s.delete_flag = FALSE
          AND (CAST(:code AS TEXT) IS NULL
               OR LOWER(s.code) LIKE LOWER(CONCAT('%', CAST(:code AS TEXT), '%')) ESCAPE '\\')
          AND (CAST(:name AS TEXT) IS NULL
               OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:name AS TEXT), '%')) ESCAPE '\\')
          AND (CAST(:status AS TEXT) IS NULL OR s.status = CAST(:status AS TEXT))
        """;

    private SampleSql() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }
}
