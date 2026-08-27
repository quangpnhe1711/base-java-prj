package com.luvina.base.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Conditional presence rule: {@code target} must have a value whenever
 * {@code field} equals {@code expected}.
 *
 * <p>The annotation is placed on the <strong>class</strong>, not on a field. A
 * field-level constraint only ever receives the field value, so it cannot read a
 * sibling field to evaluate the condition.
 *
 * <p>Values are compared by their {@code toString()}, case-insensitively, which
 * covers String, boolean, number and enum condition fields. Use the literal
 * {@code "null"} to require {@code target} when {@code field} is absent.
 *
 * <pre>
 * &#64;RequiredIf(field = "deleteAll", expected = "false", target = "ids")
 * public class BulkDeleteRequest {
 *     private boolean deleteAll;
 *     private List&lt;UUID&gt; ids;
 * }
 * </pre>
 *
 * <p>A violation is reported against {@code target}, so it appears in the
 * {@code fieldErrors} list of the error response like any other field error.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequiredIfValidator.class)
@Repeatable(RequiredIf.List.class)
@Documented
public @interface RequiredIf {

    /**
     * Name of the property holding the condition.
     *
     * @return condition property name
     */
    String field();

    /**
     * Value of {@link #field()} that activates the rule.
     *
     * @return expected condition value
     */
    String expected();

    /**
     * Name of the property that must then have a value. Strings must be
     * non-blank, collections, maps and arrays non-empty.
     *
     * @return required property name
     */
    String target();

    /**
     * Violation message. Placeholders {@code {field}}, {@code {expected}} and
     * {@code {target}} are substituted.
     *
     * @return message template
     */
    String message() default "{target} is required when {field} is {expected}";

    /**
     * Validation groups.
     *
     * @return groups this constraint belongs to
     */
    Class<?>[] groups() default { };

    /**
     * Constraint payload.
     *
     * @return payload types
     */
    Class<? extends Payload>[] payload() default { };

    /**
     * Container for several {@link RequiredIf} rules on one class.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {

        /**
         * The declared rules.
         *
         * @return nested constraints
         */
        RequiredIf[] value();
    }
}
