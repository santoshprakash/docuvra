package com.docuvra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateExcelCommentRequest(
        @NotNull Integer sheetIndex,
        @NotBlank String sheetName,
        @NotBlank String startCell,
        String endCell,
        @NotBlank String commentText
) {
}
