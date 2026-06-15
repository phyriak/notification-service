package com.phyriak.notification_orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.notification_orchestrator.service.NotificationIdempotencyService;
import com.phyriak.notification_orchestrator.service.NotificationKafkaPublisher;
import com.phyriak.payment_consumer.dto.PaymentProcessedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class NotificationKafkaListener {
    private ObjectMapper objectMapper;
    private NotificationKafkaPublisher notificationService;
    private NotificationIdempotencyService idempotencyService;


    @KafkaListener(topics = "newsletter.signup", groupId = "notification-group")
    public void listen(String message) throws Exception {
        try {
            NewsLetterSignupRequest request = objectMapper.readValue(message, NewsLetterSignupRequest.class);
            request.setEventId(idempotencyService.resolveEventId(request.getEventId()));

            if (idempotencyService.shouldSkipProcessing(request.getEventId())) {
                return;
            }

            notificationService.signup(request);

        } catch (Exception e) {
            log.error("Processing failed for message: {}", message, e);
            throw e; // Re-throw to trigger DLT
        }
    }

    @KafkaListener(
            topics = "payment",
            groupId = "notification-group"
    )
    public void listenPayment(String message) throws Exception {
        log.info("Received raw payment message: {}", message);

        try {
            PaymentProcessedEvent event =
                    objectMapper.readValue(message, PaymentProcessedEvent.class);

            if (idempotencyService.shouldSkipProcessing(event.eventId())) {
                return;
            }

            notificationService.paymentNotify(event);

        } catch (Exception e) {
            log.error("Processing failed for message: {}", message, e);
            throw e;
        }
    }
}
