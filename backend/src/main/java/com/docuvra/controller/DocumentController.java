package com.docuvra.controller;

import com.docuvra.dto.AssignDocumentRequest;
import com.docuvra.dto.AssignmentRequestResponse;
import com.docuvra.dto.ConvertedStatusResponse;
import com.docuvra.dto.DocumentAssignmentResponse;
import com.docuvra.dto.DocumentDetailsResponse;
import com.docuvra.dto.DocumentListResponse;
import com.docuvra.dto.DocumentUploadResponse;
import com.docuvra.dto.OcrStatusResponse;
import com.docuvra.dto.ThumbnailResult;
import com.docuvra.dto.UploadNewVersionResponse;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.service.DocumentService;
import com.docuvra.service.DocumentAssignmentService;
import com.docuvra.service.AssignmentRequestService;
import com.docuvra.service.OcrEligibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Tag(name = "Documents", description = "Document upload, versioning, viewing, and download APIs")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentAssignmentService documentAssignmentService;
    private final AssignmentRequestService assignmentRequestService;
    private final OcrEligibilityService ocrEligibilityService;

    @Operation(summary = "Upload a fresh document")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentUploadResponse uploadFreshDocument(@RequestPart("file") MultipartFile file) {
        return documentService.uploadFreshDocument(file);
    }

    @Operation(summary = "Upload a new version for an existing document")
    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadNewVersionResponse uploadNewVersion(
            @PathVariable UUID documentId,
            @RequestPart("file") MultipartFile file
    ) {
        return documentService.uploadNewVersion(documentId, file);
    }

    @Operation(summary = "List documents")
    @GetMapping
    public List<DocumentListResponse> listDocuments() {
        return documentService.listDocuments();
    }

    @Operation(summary = "Get document details with all versions")
    @GetMapping("/{documentId}")
    public DocumentDetailsResponse getDocumentDetails(@PathVariable UUID documentId) {
        return documentService.getDocumentDetails(documentId);
    }

    @Operation(summary = "List document assignments")
    @GetMapping("/{documentId}/assignments")
    public List<DocumentAssignmentResponse> listAssignments(@PathVariable UUID documentId) {
        return documentAssignmentService.listAssignments(documentId);
    }

    @Operation(summary = "Assign a document to a staff user")
    @PostMapping("/{documentId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentAssignmentResponse assignDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody AssignDocumentRequest request
    ) {
        return documentAssignmentService.assign(documentId, request);
    }

    @PostMapping("/{documentId}/assign")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentAssignmentResponse assignDocumentAlias(
            @PathVariable UUID documentId,
            @Valid @RequestBody AssignDocumentRequest request
    ) {
        return documentAssignmentService.assign(documentId, request);
    }

    @Operation(summary = "Remove a document assignment")
    @DeleteMapping("/{documentId}/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignment(
            @PathVariable UUID documentId,
            @PathVariable UUID assignmentId
    ) {
        documentAssignmentService.removeAssignment(documentId, assignmentId);
    }

    @DeleteMapping("/{documentId}/assign/{staffUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignmentByStaff(
            @PathVariable UUID documentId,
            @PathVariable UUID staffUserId
    ) {
        documentAssignmentService.removeAssignmentByStaff(documentId, staffUserId);
    }

    @PostMapping("/{documentId}/assignment-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentRequestResponse requestAssignment(@PathVariable UUID documentId) {
        return assignmentRequestService.requestDocument(documentId);
    }

    @Operation(summary = "Stream a document version for inline viewing")
    @GetMapping("/{documentId}/versions/{versionId}/view")
    public ResponseEntity<StreamingResponseBody> viewVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        Path viewablePdf = documentService.getViewablePdfPath(documentId, versionId);
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = Files.newInputStream(viewablePdf)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(safeSize(viewablePdf))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("viewer.pdf").build().toString())
                .body(body);
    }

    @Operation(summary = "Download a document version")
    @GetMapping("/{documentId}/versions/{versionId}/download")
    public ResponseEntity<StreamingResponseBody> downloadVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = documentService.getFileStream(documentId, versionId)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .headers(fileHeaders(version, ContentDisposition.attachment()))
                .body(body);
    }

    @Operation(summary = "Stream the original document version inline")
    @GetMapping("/{documentId}/versions/{versionId}/original/view")
    public ResponseEntity<StreamingResponseBody> viewOriginalVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = documentService.getFileStream(documentId, versionId)) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .headers(fileHeaders(version, ContentDisposition.inline()))
                .body(body);
    }

    @Operation(summary = "Get a document version thumbnail")
    @GetMapping("/{documentId}/versions/{versionId}/thumbnail")
    public ResponseEntity<byte[]> getVersionThumbnail(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        ThumbnailResult thumbnail = documentService.getThumbnail(documentId, versionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(thumbnail.mimeType()))
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                .body(thumbnail.data());
    }

    @Operation(summary = "Get converted PDF cache status")
    @GetMapping("/{documentId}/versions/{versionId}/converted/status")
    public ConvertedStatusResponse getConvertedStatus(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        return documentService.getConvertedStatus(documentId, versionId);
    }

    @Operation(summary = "Get OCR eligibility and completion status")
    @GetMapping("/{documentId}/versions/{versionId}/ocr/status")
    public OcrStatusResponse getOcrStatus(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        return ocrEligibilityService.getStatus(documentId, versionId);
    }

    @Operation(summary = "Force OCR eligibility for a document version")
    @PostMapping("/{documentId}/versions/{versionId}/ocr/force")
    public OcrStatusResponse forceOcr(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        return ocrEligibilityService.forceOcr(documentId, versionId);
    }

    @Operation(summary = "Delete converted PDF cache for one version")
    @DeleteMapping("/{documentId}/versions/{versionId}/converted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConvertedVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        documentService.deleteConvertedVersion(documentId, versionId);
    }

    @Operation(summary = "Delete converted PDF cache for one document")
    @DeleteMapping("/{documentId}/converted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConvertedDocument(@PathVariable UUID documentId) {
        documentService.deleteConvertedDocument(documentId);
    }

    @Operation(summary = "Delete all converted PDF cache files")
    @DeleteMapping("/converted")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllConverted() {
        documentService.deleteAllConverted();
    }

    @Operation(summary = "Delete a document and all versions")
    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID documentId) {
        documentService.deleteDocument(documentId);
    }

    @Operation(summary = "Delete a document version")
    @DeleteMapping("/{documentId}/versions/{versionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        documentService.deleteVersion(documentId, versionId);
    }

    private HttpHeaders fileHeaders(DocumentVersionEntity version, ContentDisposition.Builder dispositionBuilder) {
        String fileName = documentService.getDownloadFileName(version.getDocument().getId(), version.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(version.getMimeType()));
        headers.setContentLength(version.getFileSize());
        headers.setContentDisposition(dispositionBuilder.filename(fileName).build());
        return headers;
    }

    private long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (Exception exception) {
            return -1;
        }
    }
}
