package com.docuvra.dto;

public record ThumbnailResult(
        byte[] data,
        String mimeType
) {
}
