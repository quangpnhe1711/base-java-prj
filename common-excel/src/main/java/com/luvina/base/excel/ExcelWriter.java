package com.luvina.base.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.util.ReflectionUtils;

import com.luvina.base.core.exception.BusinessException;
import com.luvina.base.core.i18n.ErrorCode;

/**
 * Writes annotated objects to an {@code .xlsx} workbook.
 *
 * <p>Column order and headers come from {@link ExcelColumn}, so an export and the
 * matching import template cannot drift apart.
 *
 * <p>The whole workbook is built in memory. That is fine for the report sizes a
 * request can reasonably return; for a six-figure row count, switch to
 * {@code SXSSFWorkbook}, which streams rows to disk.
 */
public class ExcelWriter {

    private static final int MAX_AUTOSIZED_COLUMNS = 50;

    /**
     * Writes rows to a workbook.
     *
     * @param rows      objects to write
     * @param rowType   class annotated with {@link ExcelColumn}
     * @param sheetName name of the generated sheet
     * @param <T>       row type
     * @return the workbook bytes, ready to stream back or store
     */
    public <T> byte[] write(List<T> rows, Class<T> rowType, String sheetName) {
        List<Field> columns = columnsOf(rowType);
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);
            writeHeader(workbook, sheet, columns);

            int rowIndex = 1;
            for (T item : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < columns.size(); column++) {
                    Field field = columns.get(column);
                    writeCell(row.createCell(column),
                            ReflectionUtils.getField(field, item),
                            field.getAnnotation(ExcelColumn.class).pattern());
                }
            }

            autoSize(sheet, columns.size());
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.ERR015, "The workbook could not be generated");
        }
    }

    /**
     * Writes the errors of a failed import into a small workbook the user can
     * open next to the file they uploaded.
     *
     * @param errors     errors collected by {@link ExcelReader}
     * @param headers    the three column labels, already localised
     * @return the workbook bytes
     */
    public byte[] writeErrorReport(List<ExcelCellError> errors, ErrorReportHeaders headers) {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(headers.sheetName());
            Row header = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            createHeaderCell(header, 0, headers.rowLabel(), headerStyle);
            createHeaderCell(header, 1, headers.columnLabel(), headerStyle);
            createHeaderCell(header, 2, headers.messageLabel(), headerStyle);

            int rowIndex = 1;
            for (ExcelCellError error : errors) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(error.rowNumber());
                row.createCell(1).setCellValue(error.header() == null ? "" : error.header());
                row.createCell(2).setCellValue(error.message());
            }

            autoSize(sheet, 3);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.ERR015, "The error report could not be generated");
        }
    }

    private void writeHeader(Workbook workbook, Sheet sheet, List<Field> columns) {
        Row header = sheet.createRow(0);
        CellStyle style = headerStyle(workbook);
        for (int index = 0; index < columns.size(); index++) {
            createHeaderCell(header, index,
                    columns.get(index).getAnnotation(ExcelColumn.class).header(), style);
        }
    }

    private void createHeaderCell(Row row, int index, String text, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(bold);
        return style;
    }

    private void writeCell(Cell cell, Object value, String pattern) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof BigDecimal decimal) {
            // Checked before Number: BigDecimal is a Number, so the order matters.
            cell.setCellValue(decimal.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Boolean flag) {
            cell.setCellValue(flag);
        } else if (value instanceof LocalDate date) {
            cell.setCellValue(date.format(DateTimeFormatter.ofPattern(pattern)));
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime.format(DateTimeFormatter.ofPattern(pattern)));
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private List<Field> columnsOf(Class<?> rowType) {
        List<Field> columns = new ArrayList<>();
        for (Field field : rowType.getDeclaredFields()) {
            if (field.isAnnotationPresent(ExcelColumn.class)) {
                ReflectionUtils.makeAccessible(field);
                columns.add(field);
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException(
                    rowType.getName() + " has no @ExcelColumn field");
        }
        columns.sort(Comparator.comparingInt(field -> field.getAnnotation(ExcelColumn.class).order()));
        return columns;
    }

    private void autoSize(Sheet sheet, int columnCount) {
        // Auto-sizing walks every cell of the column, so it is skipped for very
        // wide sheets where the cost outweighs the cosmetic benefit.
        if (columnCount > MAX_AUTOSIZED_COLUMNS) {
            return;
        }
        for (int index = 0; index < columnCount; index++) {
            sheet.autoSizeColumn(index);
        }
    }

    /**
     * Labels used by {@link #writeErrorReport}, supplied by the caller so they
     * follow the request locale.
     *
     * @param sheetName    name of the generated sheet
     * @param rowLabel     header of the row-number column
     * @param columnLabel  header of the column-name column
     * @param messageLabel header of the message column
     */
    public record ErrorReportHeaders(String sheetName, String rowLabel, String columnLabel,
                                     String messageLabel) {

        /**
         * Returns English defaults, useful for internal tooling.
         *
         * @return default headers
         */
        public static ErrorReportHeaders defaults() {
            return new ErrorReportHeaders("Errors", "Row", "Column", "Message");
        }
    }
}
