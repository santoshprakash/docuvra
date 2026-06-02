package com.docuvra.dto;

import com.docuvra.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        NotificationType notificationType,
        String title,
        String message,
        UUID relatedDocumentId,
        UUID relatedVersionId,
        UUID relatedAnnotationId,
        UUID relatedCommentId,
        boolean read,
        LocalDateTime createdAt
) {
}
