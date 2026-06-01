package com.docuvra.repository;

import com.docuvra.entity.AnnotationCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnotationCommentRepository extends JpaRepository<AnnotationCommentEntity, UUID> {

    List<AnnotationCommentEntity> findByAnnotationIdOrderByCreatedAtAsc(UUID annotationId);
}
