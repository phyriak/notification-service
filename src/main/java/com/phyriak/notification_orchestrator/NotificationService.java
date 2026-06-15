package com.phyriak.notification_orchestrator;


import com.phyriak.email.EmailPublisher;
import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.notification_orchestrator.mapper.NotificationMapper;
import com.phyriak.notification_orchestrator.model.NotificationEntity;
import com.phyriak.notification_orchestrator.model.NotificationStatus;
import com.phyriak.notification_orchestrator.model.NotificationType;
import com.phyriak.notification_orchestrator.repository.NotificationRepository;
import com.phyriak.payment_consumer.dto.PaymentProcessedEvent;
import com.phyriak.payment_consumer.exception.PaymentEventException;
import com.phyriak.payment_consumer.exception.SignupEventException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailPublisher emailPublisher;
    private final NotificationMapper notificationMapper;

    @EventListener
    public void handleNewsletterSignup(NewsLetterSignupRequest request) {
        NotificationEntity notification = getOrCreateNewsletterNotification(request);

        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info("Signup notification already sent, skipping. eventId={}", request.getEventId());
            return;
        }

        try {
            emailPublisher.sendNewsletterSignup(request);
            setSentNotificationDetails(notification, NotificationStatus.SENT);
            log.info("Signup notification sent successfully. eventId={}", request.getEventId());

        } catch (Exception ex) {
            setFailedNotificationDetails(notification, NotificationStatus.FAILED);
            log.error(
                    "Failed to send signup notification. eventId={}",
                    request.getEventId(),
                    ex
            );
            throw new SignupEventException(
                    "Failed to process signup event: " + request.getEventId(),
                    ex
            );

        }
    }

    @EventListener
    public void handlePaymentNotification(PaymentProcessedEvent event) {
        NotificationEntity notification = getOrCreatePaymentNotification(event);

        if (notification.getStatus() == NotificationStatus.SENT) {
            log.info(
                    "Payment notification already sent, skipping. eventId={}, paymentId={}",
                    event.eventId(),
                    event.paymentId()
            );
            return;
        }

        try {
            emailPublisher.sendPaymentEmail(event);
            setSentNotificationDetails(notification, NotificationStatus.SENT);
            log.info(
                    "Payment notification sent successfully. eventId={}, paymentId={}",
                    event.eventId(),
                    event.paymentId()
            );

        } catch (Exception ex) {

            setFailedNotificationDetails(notification, NotificationStatus.FAILED);
            log.error(
                    "Failed to send payment notification. eventId={}, paymentId={}",
                    event.eventId(),
                    event.paymentId(),
                    ex
            );
            throw new PaymentEventException(
                    "Failed to process payment event: " + event.eventId(),
                    ex
            );
        }
    }

    private NotificationEntity getOrCreateNewsletterNotification(NewsLetterSignupRequest request) {
        Optional<NotificationEntity> existing =
                notificationRepository.findByEventId(request.getEventId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return notificationRepository.save(notificationMapper.toEntity(request));
        } catch (DataIntegrityViolationException ex) {
            return notificationRepository.findByEventId(request.getEventId())
                    .orElseThrow(() -> ex);
        }
    }

    private NotificationEntity getOrCreatePaymentNotification(PaymentProcessedEvent event) {
        Optional<NotificationEntity> existing = notificationRepository.findByEventId(event.eventId());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return createNotification(event);
        } catch (DataIntegrityViolationException ex) {
            return notificationRepository.findByEventId(event.eventId())
                    .orElseThrow(() -> ex);
        }
    }

    private void setSentNotificationDetails(NotificationEntity notification, NotificationStatus status) {
        notification.setStatus(status);
        notificationRepository.save(notification);
    }

    private void setFailedNotificationDetails(NotificationEntity notification, NotificationStatus status) {
        notification.setStatus(status);
        notification.setRetryCount(
                (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1
        );

        notificationRepository.save(notification);
    }

    private NotificationEntity createNotification(PaymentProcessedEvent event) {
        NotificationEntity entity = NotificationEntity.builder()
                .userId(event.userId())
                .email(event.email())
                .eventId(event.eventId())
                .type(NotificationType.PAYMENT.name())
                .subject("Payment Confirmation #" + event.paymentId())
                .message(buildNotificationMessage(event))
                .status(NotificationStatus.NEW)
                .build();

        log.info(
                "Saving payment notification. eventId={}, paymentId={}",
                event.eventId(),
                event.paymentId()
        );

        return notificationRepository.save(entity);
    }

    private String buildNotificationMessage(PaymentProcessedEvent event) {
        String orderId =
                event.orderId() != null
                        ? event.orderId().toString()
                        : "N/A";

        return """
                Payment %d for order %s was successfully processed.
                Amount: %s %s.
                Status: %s.
                """
                .formatted(
                        event.paymentId(),
                        orderId,
                        event.amount(),
                        event.currency(),
                        event.status()
                );
    }
}
