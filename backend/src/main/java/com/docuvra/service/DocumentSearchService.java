package com.docuvra.service;

import com.docuvra.dto.DocumentSearchMatchResponse;
import com.docuvra.entity.OcrPageEntity;
import com.docuvra.repository.OcrPageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DocumentSearchService {

    private final DocumentService documentService;
    private final OcrPageRepository ocrPageRepository;
    private final OcrExtractionService ocrExtractionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public List<DocumentSearchMatchResponse> searchDocumentText(UUID documentId, UUID versionId, String searchText) {
        documentService.getVersion(documentId, versionId);
        String query = searchText == null ? "" : searchText.trim();
        if (query.isBlank()) {
            return List.of();
        }
        ocrExtractionService.ensureOcrText(documentId, versionId);
        String loweredQuery = query.toLowerCase(Locale.ROOT);
        return pages(documentId, versionId).stream()
                .filter(page -> page.getExtractedText().toLowerCase(Locale.ROOT).contains(loweredQuery))
                .map(page -> new DocumentSearchMatchResponse(page.getPageNumber(), query, boxes(page, loweredQuery)))
                .toList();
    }

    @Transactional
    public List<DocumentSearchMatchResponse> searchDocumentPattern(UUID documentId, UUID versionId, String regexPattern) {
        documentService.getVersion(documentId, versionId);
        ocrExtractionService.ensureOcrText(documentId, versionId);
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        return pages(documentId, versionId).stream()
                .flatMap(page -> pattern.matcher(page.getExtractedText()).results()
                        .map(match -> toMatch(page, match)))
                .toList();
    }

    @Transactional
    public List<DocumentSearchMatchResponse> highlightDocumentText(UUID documentId, UUID versionId, String searchText) {
        return searchDocumentText(documentId, versionId, searchText);
    }

    private List<OcrPageEntity> pages(UUID documentId, UUID versionId) {
        return ocrPageRepository.findByDocumentIdAndVersionIdOrderByPageNumberAsc(documentId, versionId);
    }

    private DocumentSearchMatchResponse toMatch(OcrPageEntity page, MatchResult match) {
        return new DocumentSearchMatchResponse(page.getPageNumber(), match.group(), boxes(page, match.group().toLowerCase(Locale.ROOT)));
    }

    private List<String> boxes(OcrPageEntity page, String loweredQuery) {
        if (page.getBoxesJson() == null || page.getBoxesJson().isBlank()) {
            return List.of();
        }

        try {
            List<Map<String, Object>> parsedBoxes = objectMapper.readValue(
                    page.getBoxesJson(),
                    new TypeReference<>() {
                    }
            );
            List<String> matchedBoxes = parsedBoxes.stream()
                    .filter(box -> String.valueOf(box.getOrDefault("text", "")).toLowerCase(Locale.ROOT).contains(loweredQuery))
                    .map(this::toJson)
                    .toList();
            return matchedBoxes.isEmpty() ? List.of(page.getBoxesJson()) : matchedBoxes;
        } catch (Exception exception) {
            return List.of(page.getBoxesJson());
        }
    }

    private String toJson(Map<String, Object> box) {
        try {
            return objectMapper.writeValueAsString(box);
        } catch (Exception exception) {
            return "{}";
        }
    }
}
