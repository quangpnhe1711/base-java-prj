package com.luvina.base.core.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.luvina.base.core.exception.GlobalExceptionHandler;
import com.luvina.base.core.i18n.MessageUtil;
import com.luvina.base.core.locking.OptimisticLockSupport;
import com.luvina.base.core.validation.ValidatorWrapper;

/**
 * Entry point that registers the shared infrastructure beans.
 *
 * <p>Beans are imported explicitly rather than component-scanned, so a service
 * only has to scan its own package. Adding {@code common-core} to the classpath
 * is enough.
 *
 * <p>Every bean here is replaceable: declare your own bean of the same type in
 * the service and the service definition wins.
 */
@AutoConfiguration
// Registers the message source before Spring Boot would: its own definition
// backs off when one already exists, but only if ours is registered first.
@AutoConfigureBefore(MessageSourceAutoConfiguration.class)
@Import({
    MessageSourceConfig.class,
    WebLocaleConfig.class,
    JpaAuditConfig.class,
    MessageUtil.class,
    ValidatorWrapper.class,
    OptimisticLockSupport.class,
    GlobalExceptionHandler.class
})
public class CoreAutoConfiguration {
}
