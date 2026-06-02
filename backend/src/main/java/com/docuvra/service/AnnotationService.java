package com.docuvra.service;

import com.docuvra.dto.AnnotationCommentRequest;
import com.docuvra.dto.AnnotationCommentResponse;
import com.docuvra.dto.AnnotationRequest;
import com.docuvra.dto.AnnotationResponse;
import com.docuvra.config.SecurityProperties;
import com.docuvra.entity.AnnotationCommentEntity;
import com.docuvra.entity.AnnotationEntity;
import com.docuvra.entity.CommentMentionEntity;
import com.docuvra.entity.DocumentVersionEntity;
import com.docuvra.entity.UserEntity;
import com.docuvra.exception.AnnotationNotFoundException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.enums.NotificationType;
import com.docuvra.repository.AnnotationCommentRepository;
import com.docuvra.repository.AnnotationRepository;
import com.docuvra.repository.CommentMentionRepository;
import com.docuvra.repository.UserRepository;
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
    private final CommentMentionRepository commentMentionRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;
    private final CurrentUserService currentUserService;
    private final DocumentAccessService documentAccessService;
    private final NotificationService notificationService;
    private final SecurityProperties securityProperties;

    @Transactional
    public AnnotationResponse createAnnotation(UUID documentId, UUID versionId, AnnotationRequest request) {
        DocumentVersionEntity version = documentService.getVersion(documentId, versionId);
        UserEntity user = currentUserService.currentUserEntity();
        if (!documentAccessService.canCreateAnnotation(user)) {
            throw new ForbiddenException("Normal users cannot create annotations or root comments.");
        }
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
                .createdByUserId(user.getId())
                .createdByName(user.getUsername())
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (request.commentText() != null && !request.commentText().isBlank()) {
            AnnotationCommentEntity comment = AnnotationCommentEntity.builder()
                    .id(UUID.randomUUID())
                    .annotation(annotation)
                    .commentText(request.commentText().trim())
                    .createdByUserId(user.getId())
                    .createdByName(user.getUsername())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            annotation.getComments().add(comment);
        }

        AnnotationEntity saved = annotationRepository.save(annotation);
        if (request.mentionedUserIds() != null && !request.mentionedUserIds().isEmpty()) {
            saved.getComments().stream().findFirst().ifPresent(comment -> saveMentions(comment, request.mentionedUserIds(), now));
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> listAnnotations(UUID documentId, UUID versionId) {
        documentService.getVersion(documentId, versionId);
        UserEntity user = currentUserService.currentUserEntity();
        return annotationRepository.findByDocumentIdAndVersionIdOrderByPageNumberAscCreatedAtAsc(documentId, versionId)
                .stream()
                .filter(annotation -> canViewAnnotation(user, annotation))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnotationResponse> listPageAnnotations(UUID documentId, UUID versionId, Integer pageNumber) {
        documentService.getVersion(documentId, versionId);
        UserEntity user = currentUserService.currentUserEntity();
        return annotationRepository.findByDocumentIdAndVersionIdAndPageNumberOrderByCreatedAtAsc(documentId, versionId, pageNumber)
                .stream()
                .filter(annotation -> canViewAnnotation(user, annotation))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnnotationResponse updateAnnotation(UUID annotationId, AnnotationRequest request) {
        AnnotationEntity annotation = getAnnotation(annotationId);
        if (!documentAccessService.canCreateAnnotation(currentUserService.currentUserEntity())) {
            throw new ForbiddenException("Normal users cannot edit annotations.");
        }
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
        if (!documentAccessService.canDeleteAnnotation(currentUserService.currentUserEntity())) {
            throw new ForbiddenException("You do not have permission to delete comments or annotations.");
        }
        annotationRepository.delete(annotation);
    }

    @Transactional
    public void deleteCommentAndLinkedAnnotation(UUID commentId) {
        AnnotationCommentEntity comment = annotationCommentRepository.findById(commentId)
                .orElseThrow(() -> new AnnotationNotFoundException(commentId));
        if (!documentAccessService.canDeleteComment(currentUserService.currentUserEntity())) {
            throw new ForbiddenException("You do not have permission to delete comments or annotations.");
        }
        annotationRepository.delete(comment.getAnnotation());
    }

    @Transactional
    public AnnotationCommentResponse createComment(UUID annotationId, AnnotationCommentRequest request) {
        AnnotationEntity annotation = getAnnotation(annotationId);
        UserEntity user = currentUserService.currentUserEntity();
        if (!documentAccessService.canCreateRootComment(user) && !canNormalUserReply(user, annotation)) {
            throw new ForbiddenException("Normal users can reply only to mentioned comment threads.");
        }
        LocalDateTime now = LocalDateTime.now();
        AnnotationCommentEntity comment = AnnotationCommentEntity.builder()
                .id(UUID.randomUUID())
                .annotation(annotation)
                .commentText(request.commentText().trim())
                .createdByUserId(user.getId())
                .createdByName(user.getUsername())
                .createdAt(now)
                .updatedAt(now)
                .build();
        AnnotationCommentEntity saved = annotationCommentRepository.save(comment);
        saveMentions(saved, request.mentionedUserIds(), now);
        return toCommentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AnnotationCommentResponse> listComments(UUID annotationId) {
        if (!annotationRepository.existsById(annotationId)) {
            throw new AnnotationNotFoundException(annotationId);
        }
        return annotationCommentRepository.findByAnnotationIdOrderByCreatedAtAsc(annotationId)
                .stream()
                .filter(comment -> canViewComment(currentUserService.currentUserEntity(), comment))
                .map(this::toCommentResponse)
                .toList();
    }

    private AnnotationEntity getAnnotation(UUID annotationId) {
        return annotationRepository.findById(annotationId)
                .orElseThrow(() -> new AnnotationNotFoundException(annotationId));
    }

    private AnnotationResponse toResponse(AnnotationEntity annotation) {
        UserEntity user = currentUserService.currentUserEntity();
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
                annotation.getCreatedByUserId(),
                defaultString(annotation.getCreatedByName(), "Staff"),
                annotation.getCreatedAt(),
                annotation.getUpdatedAt(),
                annotation.getComments().stream()
                        .filter(comment -> canViewComment(user, comment))
                        .map(this::toCommentResponse)
                        .toList()
        );
    }

    private AnnotationCommentResponse toCommentResponse(AnnotationCommentEntity comment) {
        return new AnnotationCommentResponse(
                comment.getId(),
                comment.getAnnotation().getId(),
                comment.getCommentText(),
                comment.getCreatedByUserId(),
                defaultString(comment.getCreatedByName(), "Staff"),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void saveMentions(AnnotationCommentEntity comment, List<UUID> mentionedUserIds, LocalDateTime now) {
        if (mentionedUserIds == null || mentionedUserIds.isEmpty()) {
            return;
        }

        mentionedUserIds.stream()
                .distinct()
                .map(userRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(UserEntity::isActive)
                .forEach(user -> {
                    CommentMentionEntity mention = new CommentMentionEntity();
                    mention.setId(UUID.randomUUID());
                    mention.setComment(comment);
                    mention.setMentionedUser(user);
                    mention.setCreatedAt(now);
                    commentMentionRepository.save(mention);
                    notificationService.notifyUser(
                            user.getId(),
                            NotificationType.COMMENT_MENTION,
                            "You were mentioned in a comment",
                            currentUserService.currentUsername() + " mentioned you.",
                            comment.getAnnotation().getDocument().getId(),
                            comment.getAnnotation().getVersion().getId(),
                            comment.getAnnotation().getId(),
                            comment.getId()
                    );
                });
    }

    private boolean canViewAnnotation(UserEntity user, AnnotationEntity annotation) {
        if (!securityProperties.loginEnabled() && user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        if (user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        return annotation.getComments().stream().anyMatch(comment -> canViewComment(user, comment));
    }

    private boolean canViewComment(UserEntity user, AnnotationCommentEntity comment) {
        if (!securityProperties.loginEnabled() && user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        if (user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        return comment.getCreatedByUserId() != null && comment.getCreatedByUserId().equals(user.getId())
                || commentMentionRepository.existsByCommentIdAndMentionedUserId(comment.getId(), user.getId());
    }

    private boolean canNormalUserReply(UserEntity user, AnnotationEntity annotation) {
        if (!securityProperties.loginEnabled() && user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        if (user.getRole() != com.docuvra.enums.UserRole.NORMAL_USER) {
            return true;
        }
        return annotation.getComments().stream().anyMatch(comment -> canViewComment(user, comment));
    }
}
