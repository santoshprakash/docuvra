package com.docuvra.service;

import com.docuvra.dto.DocumentSearchMatchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrHighlightService {

    private final DocumentSearchService documentSearchService;

    public List<DocumentSearchMatchResponse> highlightSearchText(UUID documentId, UUID versionId, String searchText) {
        return documentSearchService.highlightDocumentText(documentId, versionId, searchText);
    }

    public List<DocumentSearchMatchResponse> highlightPattern(UUID documentId, UUID versionId, String regexPattern) {
        return documentSearchService.searchDocumentPattern(documentId, versionId, regexPattern);
    }
}
