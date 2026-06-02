package com.docuvra.repository;

import com.docuvra.entity.DocumentAssignmentEntity;
import com.docuvra.enums.DocumentAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentAssignmentRepository extends JpaRepository<DocumentAssignmentEntity, UUID> {

    boolean existsByDocumentIdAndAssignedToUserIdAndStatus(UUID documentId, UUID assignedToUserId, DocumentAssignmentStatus status);

    boolean existsByDocumentIdAndStatus(UUID documentId, DocumentAssignmentStatus status);

    Optional<DocumentAssignmentEntity> findByDocumentIdAndAssignedToUserId(UUID documentId, UUID assignedToUserId);

    List<DocumentAssignmentEntity> findAllByDocumentIdAndStatusOrderByCreatedAtDesc(UUID documentId, DocumentAssignmentStatus status);

    List<DocumentAssignmentEntity> findAllByDocumentIdAndStatus(UUID documentId, DocumentAssignmentStatus status);
}
