package com.luvina.base.excel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import jakarta.validation.Validator;

/**
 * Registers the Excel reader and writer.
 *
 * <p>The reader picks up the application {@code Validator} when one is present,
 * which is what lets Bean Validation annotations on a row class be enforced
 * during import. Without a validator it still parses; it just reports no
 * validation errors.
 */
@AutoConfiguration
public class ExcelAutoConfiguration {

    /**
     * Builds the sheet reader.
     *
     * @param validator application validator, optional
     * @return excel reader
     */
    @Bean
    @ConditionalOnMissingBean
    public ExcelReader excelReader(ObjectProvider<Validator> validator) {
        return new ExcelReader(validator.getIfAvailable());
    }

    /**
     * Builds the workbook writer.
     *
     * @return excel writer
     */
    @Bean
    @ConditionalOnMissingBean
    public ExcelWriter excelWriter() {
        return new ExcelWriter();
    }
}
