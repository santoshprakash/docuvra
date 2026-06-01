package com.docuvra.dto;

import java.util.UUID;

public record DocumentUploadResponse(
        UUID documentId,
        UUID versionId,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long fileSize,
        String status
) {
}

