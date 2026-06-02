package com.docuvra.service;

import com.docuvra.dto.OcrStatusResponse;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.enums.OcrReason;
import com.docuvra.enums.UserRole;
import com.docuvra.repository.DocumentVersionRepository;
import com.docuvra.repository.OcrPageRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OcrEligibilityService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp", "gif", "tif", "tiff", "webp");
    private static final Set<String> OFFICE_EXTENSIONS = Set.of("doc", "docx", "ppt", "pptx", "txt");
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xls", "xlsx", "csv");

    private final DocumentService documentService;
    private final FileStorageService fileStorageService;
    private final DocumentVersionRepository documentVersionRepository;
    private final OcrPageRepository ocrPageRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public OcrStatusResponse getStatus(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        return statusFor(version);
    }

    @Transactional
    public OcrStatusResponse forceOcr(UUID documentId, UUID versionId) {
        currentUserService.requireRole(UserRole.SUPERVISOR);
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        version.setOcrForced(true);
        return statusFor(documentVersionRepository.save(version));
    }

    public OcrStatusResponse statusFor(DocumentVersionEntity version) {
        String originalFileType = extension(version).toUpperCase(Locale.ROOT);
        String originalMimeType = version.getMimeType();
        boolean completed = ocrPageRepository.existsByVersionId(version.getId());

        if (version.isOcrForced()) {
            return new OcrStatusResponse(true, true, completed, true, true, originalFileType, originalMimeType, OcrReason.USER_FORCED);
        }

        String extension = originalFileType.toLowerCase(Locale.ROOT);
        if (isImage(extension, originalMimeType)) {
            return new OcrStatusResponse(true, true, completed, true, false, originalFileType, originalMimeType, OcrReason.IMAGE_FILE);
        }
        if (isExcel(extension, originalMimeType)) {
            return new OcrStatusResponse(false, false, completed, false, false, originalFileType, originalMimeType, OcrReason.EXCEL_DOCUMENT);
        }
        if (isOffice(extension, originalMimeType)) {
            return new OcrStatusResponse(false, false, completed, false, false, originalFileType, originalMimeType, OcrReason.OFFICE_DOCUMENT);
        }
        if (isPdf(extension, originalMimeType)) {
            boolean hasTextLayer = hasPdfTextLayer(version);
            return new OcrStatusResponse(
                    true,
                    !hasTextLayer,
                    completed,
                    true,
                    false,
                    originalFileType,
                    originalMimeType,
                    hasTextLayer ? OcrReason.PDF_WITH_TEXT_LAYER : OcrReason.SCANNED_PDF
            );
        }

        return new OcrStatusResponse(false, false, completed, false, false, originalFileType, originalMimeType, OcrReason.OCR_NOT_REQUIRED);
    }

    private boolean hasPdfTextLayer(DocumentVersionEntity version) {
        try {
            Path file = fileStorageService.restoreOriginalFileIfMissing(version);
            try (PDDocument document = Loader.loadPDF(file.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(1);
                stripper.setEndPage(Math.min(document.getNumberOfPages(), 3));
                return !stripper.getText(document).trim().isBlank();
            }
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isImage(String extension, String mimeType) {
        return IMAGE_EXTENSIONS.contains(extension) || lower(mimeType).startsWith("image/");
    }

    private boolean isExcel(String extension, String mimeType) {
        String value = lower(mimeType);
        return EXCEL_EXTENSIONS.contains(extension) || value.contains("spreadsheet") || value.contains("excel") || value.equals("text/csv");
    }

    private boolean isOffice(String extension, String mimeType) {
        String value = lower(mimeType);
        return OFFICE_EXTENSIONS.contains(extension)
                || value.contains("wordprocessingml")
                || value.contains("presentationml")
                || value.equals("application/msword")
                || value.equals("text/plain");
    }

    private boolean isPdf(String extension, String mimeType) {
        return "pdf".equals(extension) || "application/pdf".equalsIgnoreCase(mimeType);
    }

    private String extension(DocumentVersionEntity version) {
        String fileName = version.getOriginalFileName();
        if (fileName == null || fileName.isBlank()) {
            return "UNKNOWN";
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex < 0 || extensionIndex == fileName.length() - 1
                ? "UNKNOWN"
                : fileName.substring(extensionIndex + 1).trim();
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
