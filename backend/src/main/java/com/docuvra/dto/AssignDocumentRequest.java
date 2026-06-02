package com.docuvra.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignDocumentRequest(
        UUID userId,
        UUID staffUserId
) {
    @NotNull
    public UUID effectiveUserId() {
        return staffUserId == null ? userId : staffUserId;
    }
}
