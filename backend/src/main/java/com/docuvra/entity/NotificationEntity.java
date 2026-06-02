package com.docuvra.entity;

import com.docuvra.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class NotificationEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 60)
    private NotificationType notificationType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private String message;

    @Column(name = "related_document_id")
    private UUID relatedDocumentId;

    @Column(name = "related_version_id")
    private UUID relatedVersionId;

    @Column(name = "related_annotation_id")
    private UUID relatedAnnotationId;

    @Column(name = "related_comment_id")
    private UUID relatedCommentId;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
