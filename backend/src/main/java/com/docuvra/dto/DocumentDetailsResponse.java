package com.docuvra.dto;

import java.util.List;
import java.util.UUID;

public record DocumentDetailsResponse(
        UUID documentId,
        String title,
        Integer latestVersionNumber,
        List<DocumentVersionResponse> versions
) {
}

