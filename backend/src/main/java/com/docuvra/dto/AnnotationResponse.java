package com.docuvra.dto;

import com.docuvra.enums.AnnotationType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AnnotationResponse(
        UUID annotationId,
        UUID documentId,
        UUID versionId,
        Integer pageNumber,
        AnnotationType annotationType,
        Double xPercent,
        Double yPercent,
        Double widthPercent,
        Double heightPercent,
        Double pixelX,
        Double pixelY,
        Double pixelWidth,
        Double pixelHeight,
        Double pageRenderWidth,
        Double pageRenderHeight,
        String color,
        Double strokeWidth,
        String selectedText,
        String drawingData,
        UUID createdByUserId,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AnnotationCommentResponse> comments
) {
}
