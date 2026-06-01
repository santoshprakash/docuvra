package com.docuvra.repository;

import com.docuvra.entity.AnnotationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnotationRepository extends JpaRepository<AnnotationEntity, UUID> {

    @EntityGraph(attributePaths = "comments")
    List<AnnotationEntity> findByDocumentIdAndVersionIdOrderByPageNumberAscCreatedAtAsc(UUID documentId, UUID versionId);

    @EntityGraph(attributePaths = "comments")
    List<AnnotationEntity> findByDocumentIdAndVersionIdAndPageNumberOrderByCreatedAtAsc(
            UUID documentId,
            UUID versionId,
            Integer pageNumber
    );
}
