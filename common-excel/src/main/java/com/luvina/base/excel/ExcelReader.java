package com.luvina.base.excel;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.ReflectionUtils;

import com.luvina.base.core.exception.BusinessException;
import com.luvina.base.core.i18n.ErrorCode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * Reads a sheet into a list of annotated row objects.
 *
 * <p>The reader never stops at the first bad row. Conversion failures and Bean
 * Validation violations are collected into
 * {@link ExcelParseResult#errors()} with the spreadsheet row number attached, so
 * the caller can return a complete error report in one round trip. Only a
 * structurally wrong file, such as a missing required header, fails outright.
 *
 * <p>Both {@code .xls} and {@code .xlsx} are accepted; the format is detected
 * from the stream.
 *
 * <p>Usage:
 * <pre>
 * ExcelParseResult&lt;SampleRow&gt; result = excelReader.read(file.getInputStream(), SampleRow.class);
 * if (!result.isClean()) {
 *     return errorReport(result.errors());
 * }
 * service.importAll(result.values());
 * </pre>
 */
@RequiredArgsConstructor
public class ExcelReader {

    /** Row index, zero-based, that holds the header labels. */
    private static final int HEADER_ROW_INDEX = 0;

    /**
     * Guard against a spreadsheet whose declared last row is far beyond the real
     * data, which is a common way for an upload to exhaust memory.
     */
    private static final int MAX_DATA_ROWS = 50_000;

    private final Validator validator;

    /**
     * Reads the first sheet of a workbook.
     *
     * @param input    workbook stream; the caller keeps ownership and closes it
     * @param rowType  class annotated with {@link ExcelColumn}
     * @param <T>      row type
     * @return parsed rows and the errors found
     */
    public <T> ExcelParseResult<T> read(InputStream input, Class<T> rowType) {
        List<ExcelRow<T>> rows = new ArrayList<>();
        List<ExcelCellError> errors = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<ColumnBinding> bindings = bind(rowType, sheet.getRow(HEADER_ROW_INDEX));

            int lastRow = Math.min(sheet.getLastRowNum(), MAX_DATA_ROWS);
            for (int index = HEADER_ROW_INDEX + 1; index <= lastRow; index++) {
                Row row = sheet.getRow(index);
                if (isBlank(row, bindings)) {
                    continue;
                }
                readRow(row, rowType, bindings, rows, errors);
            }
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.ERR026, "The file could not be read as a spreadsheet");
        }
        return new ExcelParseResult<>(rows, errors);
    }

    private <T> void readRow(Row row, Class<T> rowType, List<ColumnBinding> bindings,
                             List<ExcelRow<T>> rows, List<ExcelCellError> errors) {
        int rowNumber = row.getRowNum() + 1;
        T target = instantiate(rowType);
        boolean rowFailed = false;

        for (ColumnBinding binding : bindings) {
            Cell cell = row.getCell(binding.columnIndex());
            try {
                Object value = CellConverter.convert(cell, binding.field().getType());
                ReflectionUtils.setField(binding.field(), target, value);
            } catch (IllegalArgumentException ex) {
                errors.add(new ExcelCellError(rowNumber, binding.header(), ex.getMessage()));
                rowFailed = true;
            }
        }

        if (!rowFailed) {
            rowFailed = !validate(target, bindings, rowNumber, errors);
        }
        if (!rowFailed) {
            rows.add(new ExcelRow<>(rowNumber, target));
        }
    }

    /**
     * Runs Bean Validation on a parsed row and maps each violation back to the
     * header of the field it came from, so the user sees a column name rather
     * than a Java property path.
     */
    private <T> boolean validate(T target, List<ColumnBinding> bindings, int rowNumber,
                                 List<ExcelCellError> errors) {
        if (validator == null) {
            return true;
        }
        Set<ConstraintViolation<T>> violations = validator.validate(target);
        if (violations.isEmpty()) {
            return true;
        }
        Map<String, String> headerByField = new HashMap<>();
        bindings.forEach(binding -> headerByField.put(binding.field().getName(), binding.header()));

        violations.forEach(violation -> {
            String property = violation.getPropertyPath().toString();
            errors.add(new ExcelCellError(rowNumber,
                    headerByField.get(property), violation.getMessage()));
        });
        return false;
    }

    /**
     * Matches annotated fields to header cells by text. A missing required header
     * means the wrong template was uploaded, which is a whole-file failure.
     */
    private List<ColumnBinding> bind(Class<?> rowType, Row headerRow) {
        if (headerRow == null) {
            throw new BusinessException(ErrorCode.ERR026, "The sheet has no header row");
        }

        Map<String, Integer> columnByHeader = new HashMap<>();
        for (int index = headerRow.getFirstCellNum(); index < headerRow.getLastCellNum(); index++) {
            String header = CellConverter.asText(headerRow.getCell(index));
            if (header != null) {
                columnByHeader.put(normalise(header), index);
            }
        }

        List<ColumnBinding> bindings = new ArrayList<>();
        for (Field field : rowType.getDeclaredFields()) {
            ExcelColumn column = field.getAnnotation(ExcelColumn.class);
            if (column == null) {
                continue;
            }
            Integer columnIndex = columnByHeader.get(normalise(column.header()));
            if (columnIndex == null) {
                if (column.required()) {
                    throw new BusinessException(ErrorCode.ERR026,
                            "Missing required column: " + column.header());
                }
                continue;
            }
            ReflectionUtils.makeAccessible(field);
            bindings.add(new ColumnBinding(field, column.header(), columnIndex));
        }

        if (bindings.isEmpty()) {
            throw new BusinessException(ErrorCode.ERR026,
                    "No known column was found in the uploaded file");
        }
        return bindings;
    }

    private boolean isBlank(Row row, List<ColumnBinding> bindings) {
        if (row == null) {
            return true;
        }
        return bindings.stream()
                .allMatch(binding -> CellConverter.asText(row.getCell(binding.columnIndex())) == null);
    }

    private <T> T instantiate(Class<T> rowType) {
        try {
            return rowType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(
                    rowType.getName() + " needs a public no-argument constructor", ex);
        }
    }

    private String normalise(String header) {
        return header.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** One annotated field paired with the column it was found in. */
    private record ColumnBinding(Field field, String header, int columnIndex) {
    }
}
