package com.docuvra.dto;

import java.util.UUID;

public record UploadNewVersionResponse(
        UUID documentId,
        UUID versionId,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long fileSize,
        String status
) {
}

