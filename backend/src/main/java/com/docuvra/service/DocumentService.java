package com.docuvra.service;

import com.docuvra.dto.DocumentDetailsResponse;
import com.docuvra.dto.DocumentListResponse;
import com.docuvra.dto.DocumentUploadResponse;
import com.docuvra.dto.DocumentVersionResponse;
import com.docuvra.dto.ConvertedStatusResponse;
import com.docuvra.dto.ThumbnailResult;
import com.docuvra.dto.UploadNewVersionResponse;
import com.docuvra.entity.DocumentEntity;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.enums.DocumentVersionStatus;
import com.docuvra.exception.DocumentNotFoundException;
import com.docuvra.exception.FileStorageException;
import com.docuvra.exception.InvalidFileException;
import com.docuvra.exception.LastVersionDeleteException;
import com.docuvra.exception.MaxVersionLimitException;
import com.docuvra.exception.VersionNotFoundException;
import com.docuvra.repository.DocumentRepository;
import com.docuvra.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final int MAX_VERSIONS_PER_DOCUMENT = 5;
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final FileStorageService fileStorageService;
    private final ConvertedFileStorageService convertedFileStorageService;
    private final DocumentConversionService documentConversionService;
    private final ThumbnailService thumbnailService;

    @Transactional
    public DocumentUploadResponse uploadFreshDocument(MultipartFile file) {
        validateUploadFile(file);

        UUID documentId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String originalFileName = fileStorageService.sanitizeFileName(file.getOriginalFilename());
        String title = titleFromFileName(originalFileName);
        String mimeType = detectMimeType(file);
        byte[] fileBytes = readFileBytes(file);
        Path savedPath = fileStorageService.saveFile(documentId, versionId, file);
        ThumbnailResult thumbnail = thumbnailService.generateThumbnail(file, mimeType, savedPath);

        DocumentEntity document = DocumentEntity.builder()
                .id(documentId)
                .title(title)
                .latestVersionNumber(1)
                .createdAt(now)
                .updatedAt(now)
                .build();

        DocumentVersionEntity version = DocumentVersionEntity.builder()
                .id(versionId)
                .document(document)
                .versionNumber(1)
                .originalFileName(originalFileName)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .localFilePath(savedPath.toString())
                .fileData(fileBytes)
                .thumbnailData(thumbnail.data())
                .thumbnailMimeType(thumbnail.mimeType())
                .status(DocumentVersionStatus.READY)
                .uploadedAt(now)
                .build();

        document.getVersions().add(version);
        documentRepository.save(document);
        log.info("Uploaded fresh document documentId={} versionId={} fileName={}", documentId, versionId, originalFileName);

        return new DocumentUploadResponse(
                documentId,
                versionId,
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSize(),
                version.getStatus().name()
        );
    }

    @Transactional
    public UploadNewVersionResponse uploadNewVersion(UUID documentId, MultipartFile file) {
        validateUploadFile(file);

        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        long versionCount = documentVersionRepository.countByDocumentId(documentId);
        if (versionCount >= MAX_VERSIONS_PER_DOCUMENT) {
            throw new MaxVersionLimitException(documentId);
        }

        Integer latestVersionNumber = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(DocumentVersionEntity::getVersionNumber)
                .orElse(document.getLatestVersionNumber());
        int nextVersionNumber = latestVersionNumber + 1;

        UUID versionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        String originalFileName = fileStorageService.sanitizeFileName(file.getOriginalFilename());
        String mimeType = detectMimeType(file);
        byte[] fileBytes = readFileBytes(file);
        Path savedPath = fileStorageService.saveFile(documentId, versionId, file);
        ThumbnailResult thumbnail = thumbnailService.generateThumbnail(file, mimeType, savedPath);

        DocumentVersionEntity version = DocumentVersionEntity.builder()
                .id(versionId)
                .document(document)
                .versionNumber(nextVersionNumber)
                .originalFileName(originalFileName)
                .mimeType(mimeType)
                .fileSize(file.getSize())
                .localFilePath(savedPath.toString())
                .fileData(fileBytes)
                .thumbnailData(thumbnail.data())
                .thumbnailMimeType(thumbnail.mimeType())
                .status(DocumentVersionStatus.READY)
                .uploadedAt(now)
                .build();

        document.setLatestVersionNumber(nextVersionNumber);
        document.setUpdatedAt(now);
        documentVersionRepository.save(version);
        documentRepository.save(document);
        log.info("Uploaded new version documentId={} versionId={} versionNumber={}", documentId, versionId, nextVersionNumber);

        return new UploadNewVersionResponse(
                documentId,
                versionId,
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSize(),
                version.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentListResponse> listDocuments() {
        return documentRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentDetailsResponse getDocumentDetails(UUID documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        List<DocumentVersionResponse> versions = documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(this::toVersionResponse)
                .toList();

        return new DocumentDetailsResponse(
                document.getId(),
                document.getTitle(),
                document.getLatestVersionNumber(),
                versions
        );
    }

    @Transactional(readOnly = true)
    public DocumentVersionEntity getVersion(UUID documentId, UUID versionId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }

        return documentVersionRepository.findByDocumentIdAndId(documentId, versionId)
                .orElseThrow(() -> new VersionNotFoundException(documentId, versionId));
    }

    @Transactional(readOnly = true)
    public InputStream getFileStream(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = getVersion(documentId, versionId);
        return fileStorageService.getFileStream(version);
    }

    @Transactional
    public Path getViewablePdfPath(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = getVersion(documentId, versionId);
        return documentConversionService.getOrCreateViewablePdf(version);
    }

    @Transactional(readOnly = true)
    public ConvertedStatusResponse getConvertedStatus(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = getVersion(documentId, versionId);
        boolean converted = convertedFileStorageService.convertedPdfExists(documentId, versionId);
        Path path = convertedFileStorageService.getConvertedPdfPath(documentId, versionId);
        return new ConvertedStatusResponse(
                converted,
                "viewer.pdf",
                path.toString(),
                converted ? "Converted PDF exists" : "Converted PDF has not been created"
        );
    }

    @Transactional(readOnly = true)
    public boolean isPdfOriginal(UUID documentId, UUID versionId) {
        return documentConversionService.isPdf(getVersion(documentId, versionId));
    }

    @Transactional
    public void deleteConvertedVersion(UUID documentId, UUID versionId) {
        getVersion(documentId, versionId);
        convertedFileStorageService.deleteConvertedFile(documentId, versionId);
    }

    @Transactional
    public void deleteConvertedDocument(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
        convertedFileStorageService.deleteConvertedDocumentFolder(documentId);
    }

    @Transactional
    public void deleteAllConverted() {
        convertedFileStorageService.deleteAllConvertedFiles();
    }

    @Transactional(readOnly = true)
    public ThumbnailResult getThumbnail(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = getVersion(documentId, versionId);
        if (version.getThumbnailData() != null && version.getThumbnailData().length > 0
                && version.getThumbnailMimeType() != null && !version.getThumbnailMimeType().isBlank()) {
            return new ThumbnailResult(version.getThumbnailData(), version.getThumbnailMimeType());
        }

        return thumbnailService.generatePlaceholderThumbnail(version.getMimeType(), version.getOriginalFileName());
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        documentRepository.delete(document);
        fileStorageService.deleteDocumentFolder(documentId);
        convertedFileStorageService.deleteConvertedDocumentFolder(documentId);
        log.info("Deleted document documentId={}", documentId);
    }

    @Transactional
    public void deleteVersion(UUID documentId, UUID versionId) {
        DocumentVersionEntity version = getVersion(documentId, versionId);
        long versionCount = documentVersionRepository.countByDocumentId(documentId);

        if (versionCount <= 1) {
            throw new LastVersionDeleteException(documentId);
        }

        fileStorageService.deleteVersionFile(version);
        convertedFileStorageService.deleteConvertedFile(documentId, versionId);
        documentVersionRepository.delete(version);
        documentVersionRepository.flush();

        DocumentEntity document = version.getDocument();
        Integer recalculatedLatestVersionNumber = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(DocumentVersionEntity::getVersionNumber)
                .orElse(1);
        document.setLatestVersionNumber(recalculatedLatestVersionNumber);
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
        log.info("Deleted document version documentId={} versionId={}", documentId, versionId);
    }

    @Transactional(readOnly = true)
    public String getDownloadFileName(UUID documentId, UUID versionId) {
        return getVersion(documentId, versionId).getOriginalFileName();
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File is required.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException("File size must not exceed 50 MB.");
        }
    }

    private String detectMimeType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return "application/pdf";
        }

        return DEFAULT_MIME_TYPE;
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new FileStorageException("Failed to read uploaded file bytes.", exception);
        }
    }

    private String titleFromFileName(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex);
        }
        return fileName;
    }

    private DocumentListResponse toListResponse(DocumentEntity document) {
        DocumentVersionEntity latestVersion = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(document.getId())
                .orElse(null);
        UUID latestVersionId = latestVersion == null ? null : latestVersion.getId();

        return new DocumentListResponse(
                document.getId(),
                document.getTitle(),
                document.getLatestVersionNumber(),
                latestVersionId,
                latestVersionId == null ? null : thumbnailUrl(document.getId(), latestVersionId),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersionEntity version) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getOriginalFileName(),
                version.getMimeType(),
                version.getFileSize(),
                version.getPageCount(),
                version.getStatus().name(),
                thumbnailUrl(version.getDocument().getId(), version.getId()),
                version.getUploadedAt()
        );
    }

    private String thumbnailUrl(UUID documentId, UUID versionId) {
        return "/api/documents/" + documentId + "/versions/" + versionId + "/thumbnail";
    }
}
