package com.docuvra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DocumentSearchTextRequest(
        @NotNull UUID documentId,
        @NotNull UUID versionId,
        @NotBlank String searchText
) {
}
