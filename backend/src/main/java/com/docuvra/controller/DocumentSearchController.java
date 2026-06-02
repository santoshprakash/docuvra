package com.docuvra.controller;

import com.docuvra.dto.DocumentSearchMatchResponse;
import com.docuvra.dto.DocumentSearchPatternRequest;
import com.docuvra.dto.DocumentSearchTextRequest;
import com.docuvra.service.DocumentSearchService;
import com.docuvra.service.OcrHighlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-search")
@RequiredArgsConstructor
public class DocumentSearchController {

    private final DocumentSearchService documentSearchService;
    private final OcrHighlightService ocrHighlightService;

    @PostMapping("/text")
    public List<DocumentSearchMatchResponse> searchText(@Valid @RequestBody DocumentSearchTextRequest request) {
        return documentSearchService.searchDocumentText(request.documentId(), request.versionId(), request.searchText());
    }

    @PostMapping("/pattern")
    public List<DocumentSearchMatchResponse> searchPattern(@Valid @RequestBody DocumentSearchPatternRequest request) {
        return documentSearchService.searchDocumentPattern(request.documentId(), request.versionId(), request.regexPattern());
    }

    @PostMapping("/highlight/text")
    public List<DocumentSearchMatchResponse> highlightText(@Valid @RequestBody DocumentSearchTextRequest request) {
        return ocrHighlightService.highlightSearchText(request.documentId(), request.versionId(), request.searchText());
    }

    @PostMapping("/highlight/pattern")
    public List<DocumentSearchMatchResponse> highlightPattern(@Valid @RequestBody DocumentSearchPatternRequest request) {
        return ocrHighlightService.highlightPattern(request.documentId(), request.versionId(), request.regexPattern());
    }
}
