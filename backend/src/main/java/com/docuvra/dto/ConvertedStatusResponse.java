package com.docuvra.dto;

public record ConvertedStatusResponse(
        boolean converted,
        String fileName,
        String path,
        String message
) {
}
