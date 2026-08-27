package com.luvina.base.core.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resolves the response language from the standard {@code Accept-Language}
 * header.
 *
 * <p>Locale is therefore ambient: {@code LocaleContextHolder} carries it for the
 * duration of the request and {@link com.luvina.base.core.i18n.MessageUtil} reads
 * it there. Do not add a {@code lang} query parameter, and do not thread a
 * {@code Locale} argument through service signatures.
 */
@Configuration
public class WebLocaleConfig {

    /**
     * Builds the locale resolver.
     *
     * @return resolver accepting the supported languages, defaulting to English
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(Locale.ENGLISH, Locale.forLanguageTag("vi")));
        return resolver;
    }
}
