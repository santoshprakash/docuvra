package com.docuvra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentListResponse(
        UUID documentId,
        String title,
        Integer latestVersionNumber,
        UUID latestVersionId,
        String thumbnailUrl,
        UUID uploadedByUserId,
        String uploadedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
