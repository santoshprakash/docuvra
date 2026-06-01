package com.docuvra.controller;

import com.docuvra.dto.AnnotationCommentRequest;
import com.docuvra.dto.AnnotationCommentResponse;
import com.docuvra.dto.AnnotationRequest;
import com.docuvra.dto.AnnotationResponse;
import com.docuvra.service.AnnotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;

    @PostMapping("/api/documents/{documentId}/versions/{versionId}/annotations")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnotationResponse createAnnotation(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @Valid @RequestBody AnnotationRequest request
    ) {
        return annotationService.createAnnotation(documentId, versionId, request);
    }

    @GetMapping("/api/documents/{documentId}/versions/{versionId}/annotations")
    public List<AnnotationResponse> listAnnotations(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId
    ) {
        return annotationService.listAnnotations(documentId, versionId);
    }

    @GetMapping("/api/documents/{documentId}/versions/{versionId}/pages/{pageNumber}/annotations")
    public List<AnnotationResponse> listPageAnnotations(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @PathVariable Integer pageNumber
    ) {
        return annotationService.listPageAnnotations(documentId, versionId, pageNumber);
    }

    @PutMapping("/api/annotations/{annotationId}")
    public AnnotationResponse updateAnnotation(
            @PathVariable UUID annotationId,
            @Valid @RequestBody AnnotationRequest request
    ) {
        return annotationService.updateAnnotation(annotationId, request);
    }

    @DeleteMapping("/api/annotations/{annotationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnnotation(@PathVariable UUID annotationId) {
        annotationService.deleteAnnotation(annotationId);
    }

    @DeleteMapping("/api/annotations/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCommentAndLinkedAnnotation(@PathVariable UUID commentId) {
        annotationService.deleteCommentAndLinkedAnnotation(commentId);
    }

    @PostMapping("/api/annotations/{annotationId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnotationCommentResponse createComment(
            @PathVariable UUID annotationId,
            @Valid @RequestBody AnnotationCommentRequest request
    ) {
        return annotationService.createComment(annotationId, request);
    }

    @GetMapping("/api/annotations/{annotationId}/comments")
    public List<AnnotationCommentResponse> listComments(@PathVariable UUID annotationId) {
        return annotationService.listComments(annotationId);
    }
}
