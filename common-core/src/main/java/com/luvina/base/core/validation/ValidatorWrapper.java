package com.luvina.base.core.validation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * Runs Bean Validation on objects that Spring MVC does not validate for you,
 * typically rows parsed out of an uploaded file or objects built inside a
 * service.
 *
 * <p>Violations are thrown as a {@link ConstraintViolationException}, which
 * {@link com.luvina.base.core.exception.GlobalExceptionHandler} already turns
 * into the standard error response. Controllers keep using plain
 * {@code @Valid}; this class is for everything else.
 */
@Component
@RequiredArgsConstructor
public class ValidatorWrapper {

    private final Validator validator;

    /**
     * Validates one object and throws when it has violations.
     *
     * @param target object to validate
     * @param groups validation groups to apply
     * @param <T>    object type
     * @throws ConstraintViolationException when the object is invalid
     */
    public <T> void validate(T target, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = validator.validate(target, groups);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * Validates a batch and throws once, reporting every violation found across
     * all elements rather than failing on the first bad one.
     *
     * @param targets objects to validate
     * @param groups  validation groups to apply
     * @param <T>     element type
     * @throws ConstraintViolationException when any element is invalid
     */
    public <T> void validateAll(Collection<T> targets, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = new LinkedHashSet<>();
        for (T target : targets) {
            violations.addAll(validator.validate(target, groups));
        }
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
