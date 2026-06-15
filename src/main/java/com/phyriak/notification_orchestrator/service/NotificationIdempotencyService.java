package com.phyriak.notification_orchestrator.service;

import com.phyriak.notification_orchestrator.model.NotificationStatus;
import com.phyriak.notification_orchestrator.repository.NotificationRepository;
import com.phyriak.payment_consumer.model.ProcessedEvent;
import com.phyriak.payment_consumer.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationIdempotencyService {

    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public UUID resolveEventId(UUID eventId) {
        if (eventId != null) {
            return eventId;
        }
        UUID generated = UUID.randomUUID();
        log.warn("Missing eventId in message, generated {}", generated);
        return generated;
    }

    public boolean isAlreadySent(UUID eventId) {
        return notificationRepository.existsByEventIdAndStatus(eventId, NotificationStatus.SENT);
    }

    public boolean shouldSkipProcessing(UUID eventId) {
        if (eventId == null) {
            log.error("Cannot process event without eventId");
            return true;
        }
        if (processedEventRepository.existsById(eventId)) {
            log.info("Event already processed, skipping eventId={}", eventId);
            return true;
        }
        if (isAlreadySent(eventId)) {
            log.info("Notification already sent, skipping duplicate eventId={}", eventId);
            return true;
        }
        return false;
    }

    public void markAsProcessed(UUID eventId) {
        if (eventId == null || processedEventRepository.existsById(eventId)) {
            return;
        }
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
            log.debug("Marked event as processed. eventId={}", eventId);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Event already marked as processed. eventId={}", eventId);
        }
    }
}
