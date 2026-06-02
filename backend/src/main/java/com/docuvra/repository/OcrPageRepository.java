package com.docuvra.repository;

import com.docuvra.entity.OcrPageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OcrPageRepository extends JpaRepository<OcrPageEntity, UUID> {

    boolean existsByVersionId(UUID versionId);

    List<OcrPageEntity> findByDocumentIdAndVersionIdOrderByPageNumberAsc(UUID documentId, UUID versionId);
}
