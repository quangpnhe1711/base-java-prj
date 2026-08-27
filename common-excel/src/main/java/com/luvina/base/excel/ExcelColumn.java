package com.luvina.base.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a field to a spreadsheet column.
 *
 * <p>Columns are matched by <strong>header text</strong>, not by position, so
 * reordering columns in the template does not break the import. {@link #order()}
 * only affects the column order produced by {@link ExcelWriter}.
 *
 * <pre>
 * public class SampleRow {
 *     &#64;ExcelColumn(header = "Code", order = 1, required = true)
 *     private String code;
 *
 *     &#64;ExcelColumn(header = "Effective date", order = 2)
 *     private LocalDate effectiveDate;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExcelColumn {

    /**
     * Exact header text of the column. Matching ignores case and surrounding
     * whitespace.
     *
     * @return header text
     */
    String header();

    /**
     * Position of the column when writing. Ignored when reading.
     *
     * @return zero-based-ish ordering key; ties fall back to declaration order
     */
    int order() default Integer.MAX_VALUE;

    /**
     * Whether the header must be present in the uploaded file. A missing
     * required header means the user picked the wrong template, which is
     * reported as a whole-file error rather than a per-row one.
     *
     * @return true when the header is mandatory
     */
    boolean required() default false;

    /**
     * Date and date-time pattern used when writing. Ignored when reading, where
     * the cell type is used instead.
     *
     * @return date format pattern
     */
    String pattern() default "yyyy-MM-dd";
}
