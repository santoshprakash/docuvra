package com.docuvra.repository;

import com.docuvra.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndReadFlagFalse(UUID userId);
}
