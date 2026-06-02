package com.docuvra.service;

import com.docuvra.dto.AssignDocumentRequest;
import com.docuvra.dto.DocumentAssignmentResponse;
import com.docuvra.entity.DocumentAssignmentEntity;
import com.docuvra.entity.DocumentEntity;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.DocumentAssignmentStatus;
import com.docuvra.enums.NotificationType;
import com.docuvra.enums.UserRole;
import com.docuvra.exception.DocumentNotFoundException;
import com.docuvra.exception.ForbiddenException;
import com.docuvra.exception.InvalidFileException;
import com.docuvra.repository.DocumentAssignmentRepository;
import com.docuvra.repository.DocumentRepository;
import com.docuvra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentAssignmentService {

    private final DocumentAssignmentRepository documentAssignmentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<DocumentAssignmentResponse> listAssignments(UUID documentId) {
        currentUserService.requireRole(UserRole.SUPERVISOR);
        ensureDocumentExists(documentId);
        return activeAssignments(documentId);
    }

    @Transactional
    public DocumentAssignmentResponse assign(UUID documentId, AssignDocumentRequest request) {
        currentUserService.requireRole(UserRole.SUPERVISOR);
        DocumentEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
        UserEntity assignedTo = userRepository.findById(request.effectiveUserId())
                .orElseThrow(() -> new InvalidFileException("Selected user was not found."));
        if (assignedTo.getRole() != UserRole.STAFF || !assignedTo.isActive()) {
            throw new ForbiddenException("Documents can be assigned only to active staff users.");
        }

        LocalDateTime now = LocalDateTime.now();
        UserEntity assignedBy = currentUserService.currentUserEntity();
        documentAssignmentRepository.findAllByDocumentIdAndStatus(documentId, DocumentAssignmentStatus.ASSIGNED)
                .stream()
                .filter(existing -> !existing.getAssignedToUser().getId().equals(assignedTo.getId()))
                .forEach(existing -> {
                    existing.setStatus(DocumentAssignmentStatus.REMOVED);
                    existing.setUpdatedAt(now);
                });
        DocumentAssignmentEntity assignment = documentAssignmentRepository
                .findByDocumentIdAndAssignedToUserId(documentId, request.effectiveUserId())
                .orElseGet(() -> {
                    DocumentAssignmentEntity created = new DocumentAssignmentEntity();
                    created.setId(UUID.randomUUID());
                    created.setDocument(document);
                    created.setAssignedToUser(assignedTo);
                    created.setCreatedAt(now);
                    return created;
                });
        assignment.setAssignedByUser(assignedBy);
        assignment.setStatus(DocumentAssignmentStatus.ASSIGNED);
        assignment.setUpdatedAt(now);
        DocumentAssignmentEntity saved = documentAssignmentRepository.save(assignment);
        notificationService.notifyUser(
                assignedTo.getId(),
                NotificationType.DOCUMENT_ASSIGNED,
                "Document assigned to you",
                document.getTitle() + " was assigned to you.",
                document.getId(),
                null,
                null,
                null
        );
        return toResponse(saved);
    }

    @Transactional
    public void removeAssignment(UUID documentId, UUID assignmentId) {
        currentUserService.requireRole(UserRole.SUPERVISOR);
        ensureDocumentExists(documentId);
        DocumentAssignmentEntity assignment = documentAssignmentRepository.findById(assignmentId)
                .filter(item -> item.getDocument().getId().equals(documentId))
                .orElseThrow(() -> new InvalidFileException("Assignment was not found."));
        assignment.setStatus(DocumentAssignmentStatus.REMOVED);
        assignment.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    public void removeAssignmentByStaff(UUID documentId, UUID staffUserId) {
        currentUserService.requireRole(UserRole.SUPERVISOR);
        ensureDocumentExists(documentId);
        documentAssignmentRepository.findByDocumentIdAndAssignedToUserId(documentId, staffUserId)
                .filter(assignment -> assignment.getStatus() == DocumentAssignmentStatus.ASSIGNED)
                .ifPresent(assignment -> {
                    assignment.setStatus(DocumentAssignmentStatus.REMOVED);
                    assignment.setUpdatedAt(LocalDateTime.now());
                });
    }

    private void ensureDocumentExists(UUID documentId) {
        if (!documentRepository.existsById(documentId)) {
            throw new DocumentNotFoundException(documentId);
        }
    }

    private List<DocumentAssignmentResponse> activeAssignments(UUID documentId) {
        return documentAssignmentRepository.findAllByDocumentIdAndStatusOrderByCreatedAtDesc(documentId, DocumentAssignmentStatus.ASSIGNED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DocumentAssignmentResponse toResponse(DocumentAssignmentEntity assignment) {
        return new DocumentAssignmentResponse(
                assignment.getId(),
                assignment.getAssignedToUser().getId(),
                assignment.getAssignedToUser().getUsername(),
                assignment.getAssignedToUser().getEmail(),
                assignment.getAssignedToUser().getRole(),
                assignment.getCreatedAt(),
                assignment.getAssignedByUser() == null ? null : assignment.getAssignedByUser().getUsername()
        );
    }
}
