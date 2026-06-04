package com.phyriak.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phyriak.newsletter.NewsLetterSignupEvent;
import com.phyriak.newsletter.NotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class KafkaConsumer {
    private ObjectMapper objectMapper;
    private NotificationService notificationService;


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

    @KafkaListener(topics = "payment", groupId = "payment-group")
    public void listenPayment(String message) throws Exception {
        try {
            NewsLetterSignupEvent event = objectMapper.readValue(message, NewsLetterSignupEvent.class);
            //Validation
          //action

        } catch (Exception e) {
            log.error("Processing failed for message: {}", message, e);
            throw e; // Re-throw to trigger DLT
        }
    }
}
