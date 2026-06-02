package com.docuvra.service;

import com.docuvra.dto.NotificationResponse;
import com.docuvra.dto.NotificationSummaryResponse;
import com.docuvra.entity.NotificationEntity;
import com.docuvra.entity.UserEntity;
import com.docuvra.enums.NotificationType;
import com.docuvra.repository.NotificationRepository;
import com.docuvra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void notifyUser(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID documentId,
            UUID versionId,
            UUID annotationId,
            UUID commentId
    ) {
        NotificationEntity notification = new NotificationEntity();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedDocumentId(documentId);
        notification.setRelatedVersionId(versionId);
        notification.setRelatedAnnotationId(annotationId);
        notification.setRelatedCommentId(commentId);
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifySupervisors(NotificationType type, String title, String message, UUID documentId) {
        userRepository.findAllByRoleAndActiveTrue(com.docuvra.enums.UserRole.SUPERVISOR)
                .forEach(user -> notifyUser(user.getId(), type, title, message, documentId, null, null, null));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listMine() {
        UserEntity user = currentUserService.currentUserEntity();
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationSummaryResponse summary() {
        UserEntity user = currentUserService.currentUserEntity();
        return new NotificationSummaryResponse(notificationRepository.countByUserIdAndReadFlagFalse(user.getId()));
    }

    @Transactional
    public void markRead(UUID notificationId) {
        UserEntity user = currentUserService.currentUserEntity();
        notificationRepository.findById(notificationId)
                .filter(notification -> notification.getUserId().equals(user.getId()))
                .ifPresent(notification -> notification.setReadFlag(true));
    }

    @Transactional
    public void markAllRead() {
        UserEntity user = currentUserService.currentUserEntity();
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .forEach(notification -> notification.setReadFlag(true));
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedDocumentId(),
                notification.getRelatedVersionId(),
                notification.getRelatedAnnotationId(),
                notification.getRelatedCommentId(),
                notification.isReadFlag(),
                notification.getCreatedAt()
        );
    }
}
