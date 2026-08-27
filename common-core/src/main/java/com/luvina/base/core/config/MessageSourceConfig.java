package com.luvina.base.core.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * Wires the message bundles used for API messages.
 *
 * <p>Two basenames are resolved in order:
 * <ol>
 *   <li>{@code messages/core-errors} - the shared {@code ERRxxx} catalogue owned
 *       by this module;</li>
 *   <li>{@code messages} - the per-service bundle holding domain labels and any
 *       service-specific codes.</li>
 * </ol>
 *
 * <p>Splitting them means a service can ship its own {@code messages.properties}
 * without shadowing the shared one, which a single basename would do.
 *
 * <p>Unknown keys resolve to the key itself instead of throwing, so a missing
 * translation degrades to a readable code rather than a 500.
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Builds the application message source.
     *
     * @return message source covering the shared and per-service bundles
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasenames("classpath:messages/core-errors", "classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setUseCodeAsDefaultMessage(true);
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
