package com.luvina.base.excel;

/**
 * One successfully parsed row.
 *
 * @param rowNumber one-based row number as shown in the spreadsheet
 * @param value     the populated row object
 * @param <T>       row type
 */
public record ExcelRow<T>(int rowNumber, T value) {
}
