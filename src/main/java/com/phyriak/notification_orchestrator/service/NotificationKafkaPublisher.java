package com.phyriak.notification_orchestrator.service;

import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.payment_consumer.dto.PaymentProcessedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

//Observer Pattern implementation by using event driven and kafka

@Service
public class NotificationKafkaPublisher {

    private final ApplicationEventPublisher publisher;

    public NotificationKafkaPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }


    public void signup(NewsLetterSignupRequest signupRequest) {
        publisher.publishEvent(signupRequest);
    }

    public void paymentNotify(PaymentProcessedEvent paymentEvent) {
        publisher.publishEvent(paymentEvent);
    }
}
