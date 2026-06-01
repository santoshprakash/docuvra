package com.docuvra.service;

import com.docuvra.dto.AnnotationCommentRequest;
import com.docuvra.dto.AnnotationCommentResponse;
import com.docuvra.dto.AnnotationRequest;
import com.docuvra.dto.AnnotationResponse;
import com.docuvra.entity.AnnotationCommentEntity;
import com.docuvra.entity.AnnotationEntity;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.exception.AnnotationNotFoundException;
import com.docuvra.repository.AnnotationCommentRepository;
import com.docuvra.repository.AnnotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final AnnotationCommentRepository annotationCommentRepository;
    private final DocumentService documentService;

    @Transactional
    public AnnotationResponse createAnnotation(UUID documentId, UUID versionId, AnnotationRequest request) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        LocalDateTime now = LocalDateTime.now();

        AnnotationEntity annotation = AnnotationEntity.builder()
                .id(UUID.randomUUID())
                .document(version.getDocument())
                .version(version)
                .pageNumber(request.pageNumber())
                .annotationType(request.annotationType())
                .xPercent(request.xPercent())
                .yPercent(request.yPercent())
                .widthPercent(request.widthPercent())
                .heightPercent(request.heightPercent())
                .pixelX(request.pixelX())
                .pixelY(request.pixelY())
                .pixelWidth(request.pixelWidth())
                .pixelHeight(request.pixelHeight())
                .pageRenderWidth(request.pageRenderWidth())
                .pageRenderHeight(request.pageRenderHeight())
                .color(defaultString(request.color(), "#2563eb"))
                .strokeWidth(request.strokeWidth() == null ? 2.0 : request.strokeWidth())
                .selectedText(request.selectedText())
                .drawingData(request.drawingData())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (request.commentText() != null && !request.commentText().isBlank()) {
            annotation.getComments().add(AnnotationCommentEntity.builder()
                    .id(UUID.randomUUID())
                    .annotation(annotation)
                    .commentText(request.commentText().trim())
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        return toResponse(annotationRepository.save(annotation));
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> listAnnotations(UUID documentId, UUID versionId) {
        documentService.getVersion(documentId, versionId);
        return annotationRepository.findByDocumentIdAndVersionIdOrderByPageNumberAscCreatedAtAsc(documentId, versionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> listPageAnnotations(UUID documentId, UUID versionId, Integer pageNumber) {
        documentService.getVersion(documentId, versionId);
        return annotationRepository.findByDocumentIdAndVersionIdAndPageNumberOrderByCreatedAtAsc(documentId, versionId, pageNumber)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnnotationResponse updateAnnotation(UUID annotationId, AnnotationRequest request) {
        AnnotationEntity annotation = getAnnotation(annotationId);
        annotation.setPageNumber(request.pageNumber());
        annotation.setAnnotationType(request.annotationType());
        annotation.setXPercent(request.xPercent());
        annotation.setYPercent(request.yPercent());
        annotation.setWidthPercent(request.widthPercent());
        annotation.setHeightPercent(request.heightPercent());
        annotation.setPixelX(request.pixelX());
        annotation.setPixelY(request.pixelY());
        annotation.setPixelWidth(request.pixelWidth());
        annotation.setPixelHeight(request.pixelHeight());
        annotation.setPageRenderWidth(request.pageRenderWidth());
        annotation.setPageRenderHeight(request.pageRenderHeight());
        annotation.setColor(defaultString(request.color(), annotation.getColor()));
        annotation.setStrokeWidth(request.strokeWidth() == null ? annotation.getStrokeWidth() : request.strokeWidth());
        annotation.setSelectedText(request.selectedText());
        annotation.setDrawingData(request.drawingData());
        annotation.setUpdatedAt(LocalDateTime.now());
        return toResponse(annotationRepository.save(annotation));
    }

    @Transactional
    public void deleteAnnotation(UUID annotationId) {
        AnnotationEntity annotation = getAnnotation(annotationId);
        annotationRepository.delete(annotation);
    }

    @Transactional
    public void deleteCommentAndLinkedAnnotation(UUID commentId) {
        AnnotationCommentEntity comment = annotationCommentRepository.findById(commentId)
                .orElseThrow(() -> new AnnotationNotFoundException(commentId));
        annotationRepository.delete(comment.getAnnotation());
    }

    @Transactional
    public AnnotationCommentResponse createComment(UUID annotationId, AnnotationCommentRequest request) {
        AnnotationEntity annotation = getAnnotation(annotationId);
        LocalDateTime now = LocalDateTime.now();
        AnnotationCommentEntity comment = AnnotationCommentEntity.builder()
                .id(UUID.randomUUID())
                .annotation(annotation)
                .commentText(request.commentText().trim())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return toCommentResponse(annotationCommentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<AnnotationCommentResponse> listComments(UUID annotationId) {
        if (!annotationRepository.existsById(annotationId)) {
            throw new AnnotationNotFoundException(annotationId);
        }
        return annotationCommentRepository.findByAnnotationIdOrderByCreatedAtAsc(annotationId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private AnnotationEntity getAnnotation(UUID annotationId) {
        return annotationRepository.findById(annotationId)
                .orElseThrow(() -> new AnnotationNotFoundException(annotationId));
    }

    private AnnotationResponse toResponse(AnnotationEntity annotation) {
        return new AnnotationResponse(
                annotation.getId(),
                annotation.getDocument().getId(),
                annotation.getVersion().getId(),
                annotation.getPageNumber(),
                annotation.getAnnotationType(),
                annotation.getXPercent(),
                annotation.getYPercent(),
                annotation.getWidthPercent(),
                annotation.getHeightPercent(),
                annotation.getPixelX(),
                annotation.getPixelY(),
                annotation.getPixelWidth(),
                annotation.getPixelHeight(),
                annotation.getPageRenderWidth(),
                annotation.getPageRenderHeight(),
                annotation.getColor(),
                annotation.getStrokeWidth(),
                annotation.getSelectedText(),
                annotation.getDrawingData(),
                annotation.getCreatedAt(),
                annotation.getUpdatedAt(),
                annotation.getComments().stream().map(this::toCommentResponse).toList()
        );
    }

    private AnnotationCommentResponse toCommentResponse(AnnotationCommentEntity comment) {
        return new AnnotationCommentResponse(
                comment.getId(),
                comment.getAnnotation().getId(),
                comment.getCommentText(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
