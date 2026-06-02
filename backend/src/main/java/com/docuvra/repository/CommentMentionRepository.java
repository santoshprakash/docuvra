package com.docuvra.repository;

import com.docuvra.entity.CommentMentionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentMentionRepository extends JpaRepository<CommentMentionEntity, UUID> {

    boolean existsByCommentIdAndMentionedUserId(UUID commentId, UUID mentionedUserId);

    List<CommentMentionEntity> findAllByCommentAnnotationId(UUID annotationId);
}
