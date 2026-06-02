package com.docuvra.dto;

import com.docuvra.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentAssignmentResponse(
        UUID assignmentId,
        UUID userId,
        String username,
        String email,
        UserRole role,
        LocalDateTime assignedAt,
        String assignedByName
) {
}
