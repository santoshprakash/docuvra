package com.docuvra.dto;

public record AuthResponse(
        String token,
        CurrentUserResponse user
) {
}
