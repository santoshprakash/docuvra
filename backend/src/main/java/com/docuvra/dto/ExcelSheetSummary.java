package com.docuvra.dto;

public record ExcelSheetSummary(
        int sheetIndex,
        String sheetName,
        int rowCount,
        int columnCount
) {
}
