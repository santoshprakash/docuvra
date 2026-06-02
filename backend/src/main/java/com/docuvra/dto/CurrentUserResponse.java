package com.docuvra.dto;

import com.docuvra.enums.UserRole;

import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String username,
        String email,
        String mobile,
        UserRole role,
        boolean forcePasswordChange,
        boolean loginEnabled
) {
}
