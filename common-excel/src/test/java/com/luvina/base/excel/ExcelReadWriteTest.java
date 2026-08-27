package com.luvina.base.excel;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.luvina.base.core.exception.BusinessException;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the Excel module, and at the same time the worked example of how to
 * use it: annotate a row class, write it, read it back, inspect the errors.
 */
@DisplayName("Excel read and write")
class ExcelReadWriteTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    private final ExcelWriter writer = new ExcelWriter();
    private final ExcelReader reader = new ExcelReader(VALIDATOR);

    @Test
    @DisplayName("writes rows and reads them back unchanged")
    void roundTripsRows() {
        List<PersonRow> original = List.of(
                row("Ann", 30, new BigDecimal("1200.5"), LocalDate.of(2026, 1, 15)),
                row("Bob", 41, new BigDecimal("980"), LocalDate.of(2025, 12, 1)));

        byte[] workbook = writer.write(original, PersonRow.class, "People");
        ExcelParseResult<PersonRow> result = read(workbook);

        assertThat(result.isClean()).isTrue();
        assertThat(result.values()).extracting(PersonRow::getName).containsExactly("Ann", "Bob");
        assertThat(result.values()).extracting(PersonRow::getAge).containsExactly(30, 41);
        assertThat(result.values()).extracting(PersonRow::getSalary)
                .containsExactly(new BigDecimal("1200.5"), new BigDecimal("980"));
        assertThat(result.values()).extracting(PersonRow::getStartDate)
                .containsExactly(LocalDate.of(2026, 1, 15), LocalDate.of(2025, 12, 1));
    }

    @Test
    @DisplayName("matches columns by header, not by position")
    void matchesColumnsByHeader() {
        // Columns are deliberately in a different order from the annotations.
        byte[] workbook = sheet(
                new String[] {"Age", "Full name"},
                new Object[] {"30", "Ann"});

        ExcelParseResult<PersonRow> result = read(workbook);

        assertThat(result.isClean()).isTrue();
        assertThat(result.values().get(0).getName()).isEqualTo("Ann");
        assertThat(result.values().get(0).getAge()).isEqualTo(30);
    }

    @Test
    @DisplayName("collects a conversion error per cell and keeps the good rows")
    void collectsConversionErrors() {
        byte[] workbook = sheet(
                new String[] {"Full name", "Age"},
                new Object[] {"Ann", "30"},
                new Object[] {"Bob", "not a number"},
                new Object[] {"Cid", "44"});

        ExcelParseResult<PersonRow> result = read(workbook);

        // The bad row is reported, the other two still come through, which is
        // what lets an import show every problem in one pass.
        assertThat(result.values()).extracting(PersonRow::getName).containsExactly("Ann", "Cid");
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).rowNumber()).isEqualTo(3);
        assertThat(result.errors().get(0).header()).isEqualTo("Age");
    }

    @Test
    @DisplayName("reports Bean Validation failures against the column header")
    void reportsValidationErrors() {
        byte[] workbook = sheet(
                new String[] {"Full name", "Age"},
                new Object[] {"", "-5"});

        ExcelParseResult<PersonRow> result = read(workbook);

        assertThat(result.values()).isEmpty();
        assertThat(result.errors()).extracting(ExcelCellError::header)
                .containsExactlyInAnyOrder("Full name", "Age");
    }

    @Test
    @DisplayName("skips blank rows instead of reporting them")
    void skipsBlankRows() {
        byte[] workbook = sheet(
                new String[] {"Full name", "Age"},
                new Object[] {"Ann", "30"},
                new Object[] {null, null},
                new Object[] {"Bob", "41"});

        ExcelParseResult<PersonRow> result = read(workbook);

        assertThat(result.isClean()).isTrue();
        assertThat(result.values()).hasSize(2);
    }

    @Test
    @DisplayName("rejects the whole file when a required column is missing")
    void rejectsWrongTemplate() {
        byte[] workbook = sheet(new String[] {"Age"}, new Object[] {"30"});

        assertThatThrownBy(() -> read(workbook))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Full name");
    }

    @Test
    @DisplayName("writes an error report the user can open")
    void writesErrorReport() {
        byte[] report = writer.writeErrorReport(
                List.of(new ExcelCellError(3, "Age", "must be at least 0")),
                ExcelWriter.ErrorReportHeaders.defaults());

        assertThat(report).isNotEmpty();
    }

    private ExcelParseResult<PersonRow> read(byte[] workbook) {
        return reader.read(new ByteArrayInputStream(workbook), PersonRow.class);
    }

    /** Builds a workbook from a header row and any number of data rows. */
    private byte[] sheet(String[] headers, Object[]... rows) {
        try (Workbook workbook = new XSSFWorkbook();
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Data");
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                Object[] values = rows[rowIndex];
                for (int index = 0; index < values.length; index++) {
                    if (values[index] != null) {
                        row.createCell(index).setCellValue(values[index].toString());
                    }
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private PersonRow row(String name, int age, BigDecimal salary, LocalDate startDate) {
        PersonRow person = new PersonRow();
        person.setName(name);
        person.setAge(age);
        person.setSalary(salary);
        person.setStartDate(startDate);
        return person;
    }

    /**
     * Example row class. Bean Validation annotations are enforced during import,
     * so the same rules cover the API and the spreadsheet.
     */
    @Getter
    @Setter
    public static class PersonRow {

        @ExcelColumn(header = "Full name", order = 1, required = true)
        @NotBlank
        private String name;

        @ExcelColumn(header = "Age", order = 2)
        @Min(0)
        private Integer age;

        @ExcelColumn(header = "Salary", order = 3)
        private BigDecimal salary;

        @ExcelColumn(header = "Start date", order = 4)
        private LocalDate startDate;
    }
}
