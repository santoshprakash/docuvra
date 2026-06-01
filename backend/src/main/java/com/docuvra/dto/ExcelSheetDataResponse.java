package com.docuvra.dto;

import java.util.List;

public record ExcelSheetDataResponse(
        int sheetIndex,
        String sheetName,
        int startRow,
        int rowCount,
        int totalRows,
        List<ExcelColumnResponse> columns,
        List<ExcelRowResponse> rows
) {
}
