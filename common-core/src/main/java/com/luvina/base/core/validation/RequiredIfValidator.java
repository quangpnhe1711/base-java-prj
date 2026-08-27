package com.luvina.base.core.validation;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Evaluates {@link RequiredIf} against the whole bean.
 *
 * <p>Property access goes through Spring's {@link BeanWrapper}, so it uses public
 * getters and needs no reflective access to private fields. A misspelled property
 * name fails loudly rather than silently passing validation.
 */
public class RequiredIfValidator implements ConstraintValidator<RequiredIf, Object> {

    private String conditionProperty;
    private String expectedValue;
    private String targetProperty;
    private String messageTemplate;

    @Override
    public void initialize(RequiredIf annotation) {
        this.conditionProperty = annotation.field();
        this.expectedValue = annotation.expected();
        this.targetProperty = annotation.target();
        this.messageTemplate = annotation.message();
    }

    @Override
    public boolean isValid(Object bean, ConstraintValidatorContext context) {
        if (bean == null) {
            return true;
        }

        BeanWrapper wrapper = new BeanWrapperImpl(bean);
        Object conditionValue = readProperty(wrapper, conditionProperty);

        if (!conditionMatches(conditionValue)) {
            return true;
        }

        if (hasValue(readProperty(wrapper, targetProperty))) {
            return true;
        }

        // Report the violation against the target property so that it shows up in
        // the fieldErrors list rather than as a class-level message.
        String message = messageTemplate
                .replace("{field}", conditionProperty)
                .replace("{expected}", expectedValue)
                .replace("{target}", targetProperty);

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(targetProperty)
                .addConstraintViolation();
        return false;
    }

    private Object readProperty(BeanWrapper wrapper, String property) {
        try {
            return wrapper.getPropertyValue(property);
        } catch (InvalidPropertyException ex) {
            throw new IllegalStateException(
                    "@RequiredIf on " + wrapper.getWrappedClass().getSimpleName()
                            + " refers to unknown property '" + property + "'", ex);
        }
    }

    private boolean conditionMatches(Object actual) {
        if (actual == null) {
            return "null".equalsIgnoreCase(expectedValue);
        }
        return actual.toString().equalsIgnoreCase(expectedValue);
    }

    private boolean hasValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof CharSequence text) {
            return !text.toString().isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value.getClass().isArray()) {
            return java.lang.reflect.Array.getLength(value) > 0;
        }
        return true;
    }
}
