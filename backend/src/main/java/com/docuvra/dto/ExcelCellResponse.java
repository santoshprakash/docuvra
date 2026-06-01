package com.docuvra.dto;

public record ExcelCellResponse(
        int rowIndex,
        int columnIndex,
        String cellRef,
        String value,
        String displayValue,
        String cellType
) {
}
