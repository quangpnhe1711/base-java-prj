package com.luvina.base.excel;

import java.util.List;

/**
 * Outcome of reading a sheet.
 *
 * <p>Rows and errors are returned together on purpose. An import should tell the
 * user about <em>every</em> bad row in one pass, not stop at the first one, so
 * the caller decides whether to reject the whole file or to import the valid
 * rows and hand back an error report.
 *
 * @param rows   rows that parsed and validated cleanly
 * @param errors every problem found, in row order
 * @param <T>    row type
 */
public record ExcelParseResult<T>(List<ExcelRow<T>> rows, List<ExcelCellError> errors) {

    /**
     * Tells whether the file was entirely clean.
     *
     * @return true when no error was recorded
     */
    public boolean isClean() {
        return errors.isEmpty();
    }

    /**
     * Returns just the parsed values, dropping row numbers.
     *
     * @return the valid row objects
     */
    public List<T> values() {
        return rows.stream().map(ExcelRow::value).toList();
    }
}
