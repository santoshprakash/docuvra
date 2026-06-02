package com.docuvra.dto;

import com.docuvra.enums.OcrReason;

public record OcrStatusResponse(
        boolean ocrAvailable,
        boolean ocrRequired,
        boolean ocrCompleted,
        boolean ocrEligible,
        boolean ocrForced,
        String originalFileType,
        String originalMimeType,
        OcrReason reason
) {
}
