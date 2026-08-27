package com.luvina.base.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the reference service.
 *
 * <p>Component scanning covers this package only. The shared modules register
 * themselves as auto-configurations, so there is nothing to scan or import from
 * {@code com.luvina.base.core} or {@code com.luvina.base.security}.
 */
@SpringBootApplication
public class TemplateApplication {

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }
}
