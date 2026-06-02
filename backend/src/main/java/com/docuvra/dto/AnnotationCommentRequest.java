package com.docuvra.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record AnnotationCommentRequest(
        @NotBlank String commentText,
        List<UUID> mentionedUserIds
) {
}
