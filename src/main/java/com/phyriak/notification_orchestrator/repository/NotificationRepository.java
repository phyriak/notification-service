package com.phyriak.notification_orchestrator.repository;

import com.phyriak.notification_orchestrator.model.NotificationEntity;
import com.phyriak.notification_orchestrator.model.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    Optional<NotificationEntity> findByEventId(UUID eventId);

    boolean existsByEventIdAndStatus(UUID eventId, NotificationStatus status);
}
