package com.luvina.base.excel;

/**
 * One problem found in one cell.
 *
 * @param rowNumber one-based row number as shown in the spreadsheet, so it can
 *                  be quoted back to the user directly
 * @param header    header text of the offending column, or {@code null} when the
 *                  problem concerns the whole row
 * @param message   what is wrong, already localised
 */
public record ExcelCellError(int rowNumber, String header, String message) {
}
