package com.docuvra.service;

import com.docuvra.entity.DocumentEntity;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.DocumentAssignmentStatus;
import com.docuvra.enums.UserRole;
import com.docuvra.repository.DocumentAssignmentRepository;
import com.docuvra.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentAccessService {

    private final DocumentAssignmentRepository documentAssignmentRepository;
    private final DocumentRepository documentRepository;

    public boolean isDocumentUnassigned(UUID documentId) {
        return !documentAssignmentRepository.existsByDocumentIdAndStatus(documentId, DocumentAssignmentStatus.ASSIGNED);
    }

    public boolean canViewDocument(UserEntity user, DocumentEntity document) {
        if (user.getRole() == UserRole.SUPERVISOR) {
            return true;
        }
        if (user.getRole() == UserRole.STAFF) {
            return isAssignedToUser(document.getId(), user.getId()) || isDocumentUnassigned(document.getId());
        }
        return user.getId().equals(document.getUploadedByUserId());
    }

    public boolean canCreateAnnotation(UserEntity user) {
        return user.getRole() == UserRole.SUPERVISOR || user.getRole() == UserRole.STAFF;
    }

    public boolean canCreateRootComment(UserEntity user) {
        return canCreateAnnotation(user);
    }

    public boolean canDeleteComment(UserEntity user) {
        return user.getRole() == UserRole.SUPERVISOR || user.getRole() == UserRole.STAFF;
    }

    public boolean canDeleteAnnotation(UserEntity user) {
        return user.getRole() == UserRole.SUPERVISOR || user.getRole() == UserRole.STAFF;
    }

    public boolean canAssignDocument(UserEntity user) {
        return user.getRole() == UserRole.SUPERVISOR;
    }

    public boolean canRequestAssignment(UserEntity user, UUID documentId) {
        return user.getRole() == UserRole.STAFF && isDocumentUnassigned(documentId);
    }

    public boolean isAssignedToUser(UUID documentId, UUID userId) {
        return documentAssignmentRepository.existsByDocumentIdAndAssignedToUserIdAndStatus(
                documentId,
                userId,
                DocumentAssignmentStatus.ASSIGNED
        );
    }

    public boolean canViewDocument(UserEntity user, UUID documentId) {
        return documentRepository.findById(documentId)
                .map(document -> canViewDocument(user, document))
                .orElse(false);
    }
}
