package com.docuvra.dto;

import jakarta.validation.constraints.NotBlank;

public record AnnotationCommentRequest(
        @NotBlank String commentText
) {
}
