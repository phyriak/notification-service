package com.phyriak.consumer;


import com.phyriak.model.NotificationEntity;
import com.phyriak.model.NotificationStatus;
import com.phyriak.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@AllArgsConstructor
@Slf4j
public class PaymentListener {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Async("taskExecutor")
    @EventListener
    public void handlePaymentNotification(PaymentProcessedEvent event) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        System.out.println("Sending welcome email to: " + event.paymentId());
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom("piotr_hyriak@op.pl");
        helper.setTo(event.email());
        helper.setSubject("Welcome!");
        helper.setText("Your payment " + event.orderId() + "is proceed");
        mailSender.send(message);
        //save to db
        NotificationEntity entity = NotificationEntity.builder()
                .type("PAYMENT")
                .userId(event.orderId())
                .subject("Payment processed")
                .message("Your payment for order " + event.orderId() + " was processed")
                .status(NotificationStatus.SENT)
                .sentAt(
                        LocalDateTime.ofInstant(
                                event.processedAt(),
                                ZoneId.systemDefault()
                        ))
                .email(event.email())
                .build();
        log.info("Save " + event.eventId() + " to DB");
        notificationRepository.save(entity);
    }
}
