package com.luvina.base.template.entity;

import java.time.LocalDate;

import com.luvina.base.core.entity.BaseEntity;
import com.luvina.base.template.enums.SampleStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Reference entity.
 *
 * <p>Shows the conventions every entity follows: extend
 * {@link BaseEntity} for the id, audit columns and soft delete; map to a
 * {@code t_} table for transactional data or {@code m_} for master data; name
 * columns in snake_case explicitly rather than relying on the naming strategy.
 *
 * <p>Uniqueness of {@code code} is enforced by a partial index in the migration,
 * not here, because it must ignore soft-deleted rows.
 */
@Entity
@Table(name = "t_samples")
@Getter
@Setter
public class Sample extends BaseEntity {

    /** Business identifier, unique among non-deleted rows. */
    @Column(name = "code", nullable = false, length = 32)
    private String code;

    /** Display name. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Free-text description. */
    @Column(name = "description", length = 1000)
    private String description;

    /** Lifecycle state. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private SampleStatus status = SampleStatus.ACTIVE;

    /** Date the record takes effect. */
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
}
