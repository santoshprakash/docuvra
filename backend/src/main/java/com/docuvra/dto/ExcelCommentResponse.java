package com.docuvra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExcelCommentResponse(
        UUID id,
        UUID documentId,
        UUID versionId,
        int sheetIndex,
        String sheetName,
        String startCell,
        String endCell,
        int startRow,
        int startColumn,
        Integer endRow,
        Integer endColumn,
        String commentText,
        String createdByName,
        LocalDateTime createdAt
) {
}
