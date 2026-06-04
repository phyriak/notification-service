package com.phyriak.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phyriak.newsletter.NewsLetterSignupEvent;
import com.phyriak.newsletter.NotificationService;
import com.phyriak.repository.ProcessedEventRepository;
import com.phyriak.repository.model.ProcessedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@AllArgsConstructor
public class KafkaConsumer {
    private ObjectMapper objectMapper;
    private NotificationService notificationService;
    private ProcessedEventRepository eventRepository;


    @KafkaListener(topics = "newsletter.signup", groupId = "notification-group")
    public void listen(String message) throws Exception {
        try {
            NewsLetterSignupEvent event = objectMapper.readValue(message, NewsLetterSignupEvent.class);
            //Validation
            notificationService.signup(event);

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
        try {
            PaymentProcessedEvent event =
                    objectMapper.readValue(message, PaymentProcessedEvent.class);
            //Validation
            if (eventRepository.existsById(event.eventId())) {
                log.info("Duplicate event {}", event.eventId());
                return;
            }

            //todo when fails add to ddl later in next steps
            notificationService.paymentNotify(event);

            eventRepository.save(
                    new ProcessedEvent(
                            event.eventId(),
                            Instant.now()
                    )
            );
        } catch (Exception e) {
            log.error("Processing failed for message: {}", message, e);
            throw e;
        }
    }
}
