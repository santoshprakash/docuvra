package com.docuvra.repository;

import com.docuvra.entity.DocumentAssignmentRequestEntity;
import com.docuvra.enums.DocumentAssignmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentAssignmentRequestRepository extends JpaRepository<DocumentAssignmentRequestEntity, UUID> {

    Optional<DocumentAssignmentRequestEntity> findByDocumentIdAndRequestedByUserIdAndStatus(
            UUID documentId,
            UUID requestedByUserId,
            DocumentAssignmentRequestStatus status
    );

    List<DocumentAssignmentRequestEntity> findAllByStatusOrderByRequestedAtDesc(DocumentAssignmentRequestStatus status);
}
