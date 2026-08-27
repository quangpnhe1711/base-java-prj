package com.luvina.base.template.enums;

/**
 * Lifecycle state of a sample.
 *
 * <p>Persisted by name through {@code @Enumerated(EnumType.STRING)}. Never
 * persist an enum by ordinal: reordering the constants would silently rewrite
 * the meaning of existing rows.
 */
public enum SampleStatus {

    /** Visible and usable. */
    ACTIVE,

    /** Retained for history but no longer selectable. */
    INACTIVE
}
