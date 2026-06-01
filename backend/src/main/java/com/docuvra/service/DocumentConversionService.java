package com.docuvra.service;

import com.docuvra.dto.ThumbnailResult;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.exception.ConversionException;
import com.docuvra.exception.UnsupportedConversionException;
import com.docuvra.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentConversionService {

    private static final Set<String> OFFICE_EXTENSIONS = Set.of(
            "doc", "docx", "ppt", "pptx", "txt", "rtf", "odt", "odp"
    );
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "bmp", "gif", "tiff", "tif", "webp"
    );

    private final FileStorageService fileStorageService;
    private final ConvertedFileStorageService convertedFileStorageService;
    private final OfficeConverterService officeConverterService;
    private final ImageConversionService imageConversionService;
    private final ThumbnailService thumbnailService;
    private final DocumentVersionRepository documentVersionRepository;

    @Transactional
    public Path getOrCreateViewablePdf(DocumentVersionEntity version) {
        if (isPdf(version)) {
            return fileStorageService.restoreOriginalFileIfMissing(version);
        }

        UUID documentId = version.getDocument().getId();
        UUID versionId = version.getId();
        if (convertedFileStorageService.convertedPdfExists(documentId, versionId)) {
            return convertedFileStorageService.getConvertedPdfPath(documentId, versionId);
        }

        Path originalFile = fileStorageService.restoreOriginalFileIfMissing(version);
        Path convertedPdf = convertToPdf(version, originalFile);
        validateConvertedPdf(convertedPdf);
        refreshThumbnail(version, convertedPdf);
        return convertedPdf;
    }

    public boolean isPdf(DocumentVersionEntity version) {
        return "application/pdf".equalsIgnoreCase(version.getMimeType())
                || extension(version).equals("pdf");
    }

    public boolean isOfficeOrTextFile(DocumentVersionEntity version) {
        return OFFICE_EXTENSIONS.contains(extension(version));
    }

    public boolean isImageFile(DocumentVersionEntity version) {
        return IMAGE_EXTENSIONS.contains(extension(version));
    }

    public Path convertToPdf(DocumentVersionEntity version, Path originalFile) {
        UUID documentId = version.getDocument().getId();
        UUID versionId = version.getId();
        Path outputDir = convertedFileStorageService.ensureConvertedFolder(documentId, versionId);
        Path finalPdfPath = convertedFileStorageService.getConvertedPdfPath(documentId, versionId);

        if (isOfficeOrTextFile(version)) {
            Path workDir = outputDir.resolve("work");
            return convertOfficeToPdf(originalFile, workDir, finalPdfPath);
        }

        if (isImageFile(version)) {
            return convertImageToPdf(originalFile, finalPdfPath);
        }

        throw new UnsupportedConversionException();
    }

    public Path convertOfficeToPdf(Path inputFile, Path outputDir, Path finalPdfPath) {
        return officeConverterService.convert(inputFile, outputDir, finalPdfPath);
    }

    public Path convertImageToPdf(Path inputFile, Path outputPdf) {
        return imageConversionService.convertImageToPdf(inputFile, outputPdf);
    }

    public void validateConvertedPdf(Path pdfPath) {
        try {
            if (!Files.isRegularFile(pdfPath) || Files.size(pdfPath) == 0) {
                throw new ConversionException("Document preview conversion failed. Please try again or download the original file.");
            }
            try (PDDocument ignored = Loader.loadPDF(pdfPath.toFile())) {
                // PDFBox validation is enough here; the loaded document is closed immediately.
            }
        } catch (IOException exception) {
            throw new ConversionException("Document preview conversion failed. Please try again or download the original file.", exception);
        }
    }

    private void refreshThumbnail(DocumentVersionEntity version, Path convertedPdf) {
        try {
            ThumbnailResult thumbnail = thumbnailService.generateThumbnailFromPdf(convertedPdf);
            version.setThumbnailData(thumbnail.data());
            version.setThumbnailMimeType(thumbnail.mimeType());
            documentVersionRepository.save(version);
        } catch (Exception exception) {
            log.warn("Converted thumbnail refresh failed versionId={}", version.getId(), exception);
        }
    }

    private String extension(DocumentVersionEntity version) {
        String fileName = version.getOriginalFileName();
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(extensionIndex + 1).toLowerCase(Locale.ROOT);
    }
}
