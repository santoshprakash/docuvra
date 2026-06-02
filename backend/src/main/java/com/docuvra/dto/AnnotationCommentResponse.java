package com.docuvra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnotationCommentResponse(
        UUID commentId,
        UUID annotationId,
        String commentText,
        UUID createdByUserId,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
