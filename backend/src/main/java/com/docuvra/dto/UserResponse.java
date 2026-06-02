package com.docuvra.dto;

import com.docuvra.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String email,
        String mobile,
        UserRole role,
        boolean active,
        boolean forcePasswordChange,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
