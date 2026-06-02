package com.docuvra.dto;

import java.util.List;
import java.util.UUID;

public record DocumentDetailsResponse(
        UUID documentId,
        String title,
        Integer latestVersionNumber,
        UUID uploadedByUserId,
        String uploadedByName,
        List<DocumentAssignmentResponse> assignments,
        List<DocumentVersionResponse> versions
) {
}
