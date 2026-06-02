package com.docuvra.dto;

import com.docuvra.enums.DocumentAssignmentRequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentRequestResponse(
        UUID requestId,
        UUID documentId,
        String documentTitle,
        UUID requestedByUserId,
        String requestedByUsername,
        DocumentAssignmentRequestStatus status,
        LocalDateTime requestedAt,
        String reviewComment
) {
}
