package com.phyriak.newsletter;

import com.phyriak.consumer.PaymentProcessedEvent;
import com.phyriak.repository.model.ProcessedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

//Observer Pattern implementation by using event driven and kafka

@Service
public class NotificationService {

    private final ApplicationEventPublisher publisher;

    public NotificationService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }


    public void signup(NewsLetterSignupEvent signupEvent){
        publisher.publishEvent(signupEvent);
    }

    public void paymentNotify(Object event){
        publisher.publishEvent(event);
    }
}
