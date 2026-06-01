package com.docuvra.repository;

import com.docuvra.entity.ExcelCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExcelCommentRepository extends JpaRepository<ExcelCommentEntity, UUID> {

    List<ExcelCommentEntity> findByDocumentIdAndVersionIdAndSheetIndexOrderByCreatedAtAsc(
            UUID documentId,
            UUID versionId,
            Integer sheetIndex
    );

    List<ExcelCommentEntity> findByDocumentIdAndVersionIdOrderBySheetIndexAscCreatedAtAsc(UUID documentId, UUID versionId);
}
