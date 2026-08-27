-- Reference migration.
--
-- Naming: V{n}__{snake_case_description}.sql, versions strictly increasing and
-- never reused. An applied migration is immutable: to change something, add the
-- next version. Editing an applied file breaks Flyway's checksum validation for
-- everyone who already ran it.
--
-- Table prefixes: t_ for transactional data, m_ for master data.

CREATE TABLE t_samples (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    code            VARCHAR(32)  NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     VARCHAR(1000),
    status          VARCHAR(16)  NOT NULL,
    effective_date  DATE,

    -- Audit and soft-delete columns of BaseEntity. Every table has them.
    created_user_id UUID,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_user_id UUID,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    delete_flag     BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_samples PRIMARY KEY (id)
);

-- Uniqueness applies to live rows only: a soft-deleted row must not block
-- reusing its code. A partial index is what makes that possible, and it is why
-- the rule cannot be expressed as a plain UNIQUE constraint on the column.
CREATE UNIQUE INDEX ux_samples_code_active
    ON t_samples (code)
    WHERE delete_flag = FALSE;

-- Supports the paged search, which always filters on delete_flag and orders by
-- updated_at.
CREATE INDEX ix_samples_active_updated_at
    ON t_samples (updated_at DESC)
    WHERE delete_flag = FALSE;

COMMENT ON TABLE t_samples IS 'Reference table demonstrating the entity conventions';
