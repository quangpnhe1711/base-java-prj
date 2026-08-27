package com.luvina.base.core.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Thin facade over Spring's {@link MessageSource}.
 *
 * <p>Overloads without a {@code Locale} resolve it from
 * {@link LocaleContextHolder}, which Spring populates from the
 * {@code Accept-Language} header. Prefer those: passing a {@code Locale} through
 * every service method signature is noise.
 */
@Component
@RequiredArgsConstructor
public class MessageUtil {

    private final MessageSource messageSource;

    /**
     * Resolves an error message for the request locale.
     *
     * @param code error code
     * @param args placeholder arguments, may be {@code null}
     * @return localised message
     */
    public String getMessage(ErrorCode code, Object... args) {
        return getMessage(code.name(), args, LocaleContextHolder.getLocale());
    }

    /**
     * Resolves a label for the request locale.
     *
     * @param key label key
     * @return localised label
     */
    public String getMessage(MessageKey key) {
        return getMessage(key.key(), null, LocaleContextHolder.getLocale());
    }

    /**
     * Resolves an arbitrary message key for an explicit locale.
     *
     * @param code   message key
     * @param args   placeholder arguments, may be {@code null}
     * @param locale target locale
     * @return localised message, or the key itself when unmapped
     */
    public String getMessage(String code, Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * Resolves an error message whose single placeholder is a localised label.
     *
     * <p>{@code format(ERR001, SAMPLE_NAME)} yields "Sample name is required."
     *
     * @param code   error code
     * @param target label substituted as {@code {0}}
     * @return localised message
     */
    public String format(ErrorCode code, MessageKey target) {
        return getMessage(code, getMessage(target));
    }

    /**
     * Resolves an error message whose two placeholders are localised labels.
     *
     * @param code    error code
     * @param first   label substituted as {@code {0}}
     * @param second  label substituted as {@code {1}}
     * @return localised message
     */
    public String format(ErrorCode code, MessageKey first, MessageKey second) {
        return getMessage(code, getMessage(first), getMessage(second));
    }
}
