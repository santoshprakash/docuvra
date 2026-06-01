package com.docuvra.controller;

import com.docuvra.dto.CreateExcelCommentRequest;
import com.docuvra.dto.ExcelCommentResponse;
import com.docuvra.dto.ExcelSheetDataResponse;
import com.docuvra.dto.ExcelWorkbookResponse;
import com.docuvra.service.ExcelPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ExcelPreviewController {

    private final ExcelPreviewService excelPreviewService;

    @GetMapping("/api/documents/{documentId}/versions/{versionId}/excel/workbook")
    public ExcelWorkbookResponse getWorkbook(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        return excelPreviewService.getWorkbook(documentId, versionId);
    }

    @GetMapping("/api/documents/{documentId}/versions/{versionId}/excel/sheets/{sheetIndex}")
    public ExcelSheetDataResponse getSheet(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @PathVariable int sheetIndex,
            @RequestParam(defaultValue = "0") int startRow,
            @RequestParam(defaultValue = "200") int limit
    ) {
        return excelPreviewService.getSheet(documentId, versionId, sheetIndex, startRow, limit);
    }

    @GetMapping("/api/documents/{documentId}/versions/{versionId}/excel/sheets/{sheetIndex}/comments")
    public List<ExcelCommentResponse> getComments(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @PathVariable int sheetIndex
    ) {
        return excelPreviewService.listSheetComments(documentId, versionId, sheetIndex);
    }

    @PostMapping("/api/documents/{documentId}/versions/{versionId}/excel/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ExcelCommentResponse createComment(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @Valid @RequestBody CreateExcelCommentRequest request
    ) {
        return excelPreviewService.createComment(documentId, versionId, request);
    }

    @DeleteMapping("/api/excel-comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable UUID commentId) {
        excelPreviewService.deleteComment(commentId);
    }
}
