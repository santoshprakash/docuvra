package com.docuvra.dto;

import java.util.List;

public record ExcelRowResponse(
        int rowIndex,
        List<ExcelCellResponse> cells
) {
}
