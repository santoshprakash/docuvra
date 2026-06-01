package com.docuvra.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID versionId,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long fileSize,
        Integer pageCount,
        String status,
        String thumbnailUrl,
        LocalDateTime uploadedAt
) {
}
