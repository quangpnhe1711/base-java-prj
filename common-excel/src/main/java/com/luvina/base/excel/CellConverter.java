package com.luvina.base.excel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;

import com.luvina.base.core.constant.AppConstants;

/**
 * Converts a spreadsheet cell to the target field type.
 *
 * <p>Numeric cells are read through their numeric value rather than their
 * displayed text, so an id typed as {@code 100} does not arrive as
 * {@code "100.0"}. Everything else goes through {@link DataFormatter}, which
 * renders the cell the way the user sees it.
 *
 * <p>Unconvertible input raises {@link IllegalArgumentException}; the reader
 * turns that into a per-cell error rather than failing the whole file.
 */
final class CellConverter {

    private static final DataFormatter FORMATTER = new DataFormatter();

    private CellConverter() {
        throw new UnsupportedOperationException(AppConstants.UTILITY_CLASS_ERROR);
    }

    /**
     * Reads a cell as trimmed text, or {@code null} when the cell is blank.
     */
    static String asText(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            double value = cell.getNumericCellValue();
            // Render whole numbers without the trailing ".0" that a plain
            // toString would add.
            if (value == Math.rint(value) && !Double.isInfinite(value)) {
                return String.valueOf((long) value);
            }
            return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        }
        String text = FORMATTER.formatCellValue(cell).trim();
        return text.isEmpty() ? null : text;
    }

    /**
     * Converts a cell to the requested type.
     *
     * @param cell       cell to read, may be {@code null}
     * @param targetType field type
     * @return converted value, or {@code null} when the cell is blank
     * @throws IllegalArgumentException when the text does not fit the type
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object convert(Cell cell, Class<?> targetType) {
        // Temporal types read the cell value directly: text rendering depends on
        // the workbook locale and would not round-trip.
        if (cell != null && cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
            if (dateTime == null) {
                return null;
            }
            if (targetType == LocalDate.class) {
                return dateTime.toLocalDate();
            }
            if (targetType == LocalDateTime.class) {
                return dateTime;
            }
        }

        String text = asText(cell);
        if (text == null) {
            return null;
        }

        if (targetType == String.class) {
            return text;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(text);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(text);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(text);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(text);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return parseBoolean(text);
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(text);
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(text);
        }
        if (targetType == UUID.class) {
            return UUID.fromString(text);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<Enum>) targetType, text.toUpperCase(Locale.ROOT));
        }
        throw new IllegalArgumentException("Unsupported column type: " + targetType.getName());
    }

    private static Boolean parseBoolean(String text) {
        String normalised = text.toLowerCase(Locale.ROOT);
        return switch (normalised) {
            case "true", "yes", "y", "1", "x" -> Boolean.TRUE;
            case "false", "no", "n", "0", "" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Not a boolean value: " + text);
        };
    }
}
