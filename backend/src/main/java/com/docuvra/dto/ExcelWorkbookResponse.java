package com.docuvra.dto;

import java.util.List;
import java.util.UUID;

public record ExcelWorkbookResponse(
        UUID documentId,
        UUID versionId,
        String fileName,
        List<ExcelSheetSummary> sheets
) {
}
