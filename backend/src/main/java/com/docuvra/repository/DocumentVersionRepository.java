package com.docuvra.repository;

import com.docuvra.entity.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersionEntity> findByDocumentIdAndId(UUID documentId, UUID id);

    long countByDocumentId(UUID documentId);

    Optional<DocumentVersionEntity> findTopByDocumentIdOrderByVersionNumberDesc(UUID documentId);
}

