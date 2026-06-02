package com.docuvra.dto;

import java.util.List;

public record DocumentSearchMatchResponse(
        Integer pageNumber,
        String matchedText,
        List<String> boxes
) {
}
