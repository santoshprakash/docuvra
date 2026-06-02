package com.docuvra.dto;

import com.docuvra.enums.AnnotationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AnnotationRequest(
        @NotNull @Min(1) Integer pageNumber,
        @NotNull AnnotationType annotationType,
        @NotNull Double xPercent,
        @NotNull Double yPercent,
        @NotNull Double widthPercent,
        @NotNull Double heightPercent,
        @NotNull Double pixelX,
        @NotNull Double pixelY,
        @NotNull Double pixelWidth,
        @NotNull Double pixelHeight,
        @NotNull Double pageRenderWidth,
        @NotNull Double pageRenderHeight,
        String color,
        Double strokeWidth,
        String selectedText,
        String drawingData,
        String commentText,
        List<UUID> mentionedUserIds
) {
}
