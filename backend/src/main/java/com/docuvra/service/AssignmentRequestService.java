package com.docuvra.service;

import com.docuvra.config.SecurityProperties;
import com.docuvra.dto.AssignDocumentRequest;
import com.docuvra.dto.AssignmentRequestResponse;
import com.docuvra.dto.RejectAssignmentRequest;
import com.docuvra.entity.DocumentAssignmentRequestEntity;
import com.docuvra.entity.DocumentEntity;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.DocumentAssignmentRequestStatus;
import com.docuvra.enums.NotificationType;
import com.docuvra.enums.UserRole;
import com.docuvra.exception.DocumentNotFoundException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.repository.DocumentAssignmentRequestRepository;
import com.docuvra.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentRequestService {

    private final DocumentAssignmentRequestRepository requestRepository;
    private final DocumentRepository documentRepository;
    private final CurrentUserService currentUserService;
    private final DocumentAccessService documentAccessService;
    private final DocumentAssignmentService documentAssignmentService;
    private final NotificationService notificationService;
    private final SecurityProperties securityProperties;

    @Transactional
    public AssignmentRequestResponse requestDocument(UUID documentId) {
        ensureAssignmentRequestsEnabled();
        UserEntity user = currentUserService.currentUserEntity();
        if (user.getRole() != UserRole.STAFF) {
            throw new ForbiddenException("Only staff users can request documents.");
        }
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        if (!documentAccessService.canRequestAssignment(user, documentId)) {
            throw new ForbiddenException("This document is already assigned.");
        }

        DocumentAssignmentRequestEntity request = requestRepository
                .findByDocumentIdAndRequestedByUserIdAndStatus(documentId, user.getId(), DocumentAssignmentRequestStatus.PENDING)
                .orElseGet(() -> {
                    DocumentAssignmentRequestEntity created = new DocumentAssignmentRequestEntity();
                    created.setId(UUID.randomUUID());
                    created.setDocument(document);
                    created.setRequestedByUser(user);
                    created.setStatus(DocumentAssignmentRequestStatus.PENDING);
                    created.setRequestedAt(LocalDateTime.now());
                    notificationService.notifySupervisors(
                            NotificationType.ASSIGNMENT_REQUEST_CREATED,
                            "Assignment request created",
                            user.getUsername() + " requested access to " + document.getTitle() + ".",
                            document.getId()
                    );
                    return created;
                });
        return toResponse(requestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<AssignmentRequestResponse> pendingRequests() {
        ensureAssignmentRequestsEnabled();
        currentUserService.requireRole(UserRole.SUPERVISOR);
        return requestRepository.findAllByStatusOrderByRequestedAtDesc(DocumentAssignmentRequestStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AssignmentRequestResponse approve(UUID requestId) {
        ensureAssignmentRequestsEnabled();
        currentUserService.requireRole(UserRole.SUPERVISOR);
        UserEntity supervisor = currentUserService.currentUserEntity();
        DocumentAssignmentRequestEntity request = getPending(requestId);
        request.setStatus(DocumentAssignmentRequestStatus.APPROVED);
        request.setReviewedByUser(supervisor);
        request.setReviewedAt(LocalDateTime.now());
        documentAssignmentService.assign(request.getDocument().getId(), new AssignDocumentRequest(request.getRequestedByUser().getId(), null));
        notificationService.notifyUser(
                request.getRequestedByUser().getId(),
                NotificationType.ASSIGNMENT_REQUEST_APPROVED,
                "Assignment request approved",
                request.getDocument().getTitle() + " was added to your bucket.",
                request.getDocument().getId(),
                null,
                null,
                null
        );
        return toResponse(request);
    }

    @Transactional
    public AssignmentRequestResponse reject(UUID requestId, RejectAssignmentRequest rejectRequest) {
        ensureAssignmentRequestsEnabled();
        currentUserService.requireRole(UserRole.SUPERVISOR);
        UserEntity supervisor = currentUserService.currentUserEntity();
        DocumentAssignmentRequestEntity request = getPending(requestId);
        request.setStatus(DocumentAssignmentRequestStatus.REJECTED);
        request.setReviewedByUser(supervisor);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewComment(rejectRequest == null ? null : rejectRequest.reviewComment());
        notificationService.notifyUser(
                request.getRequestedByUser().getId(),
                NotificationType.ASSIGNMENT_REQUEST_REJECTED,
                "Assignment request rejected",
                request.getReviewComment() == null || request.getReviewComment().isBlank()
                        ? request.getDocument().getTitle() + " request was rejected."
                        : request.getReviewComment(),
                request.getDocument().getId(),
                null,
                null,
                null
        );
        return toResponse(request);
    }

    private DocumentAssignmentRequestEntity getPending(UUID requestId) {
        return requestRepository.findById(requestId)
                .filter(request -> request.getStatus() == DocumentAssignmentRequestStatus.PENDING)
                .orElseThrow(() -> new ForbiddenException("Pending assignment request was not found."));
    }

    private AssignmentRequestResponse toResponse(DocumentAssignmentRequestEntity request) {
        return new AssignmentRequestResponse(
                request.getId(),
                request.getDocument().getId(),
                request.getDocument().getTitle(),
                request.getRequestedByUser().getId(),
                request.getRequestedByUser().getUsername(),
                request.getStatus(),
                request.getRequestedAt(),
                request.getReviewComment()
        );
    }

    private void ensureAssignmentRequestsEnabled() {
        if (!securityProperties.loginEnabled()) {
            throw new ForbiddenException("Assignment requests are disabled while login is disabled.");
        }
    }
}
