package com.phyriak.consumer;


import com.phyriak.consumer.excpetion.PaymentEventFailException;
import com.phyriak.model.NotificationEntity;
import com.phyriak.model.NotificationStatus;
import com.phyriak.repository.NotificationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentListener {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final NotificationProperties properties;

    @EventListener
    @Transactional
    public void handlePaymentNotification(PaymentProcessedEvent event) {
        //save to db
        NotificationEntity notification = createNotification(event);

        try {
            sendPaymentEmail(event);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());

            notificationRepository.save(notification);

            log.info(
                    "Payment notification sent successfully. eventId={}, paymentId={}",
                    event.eventId(),
                    event.paymentId()
            );

        } catch (Exception ex) {

            notification.setStatus(NotificationStatus.FAILED);

            notification.setRetryCount(
                    (notification.getRetryCount() == null ? 0 : notification.getRetryCount()) + 1
            );

            notificationRepository.save(notification);

            log.error(
                    "Failed to send payment notification. eventId={}, paymentId={}",
                    event.eventId(),
                    event.paymentId(),
                    ex
            );
            throw new PaymentEventFailException(
                    "Failed to process payment event: " + event.eventId(),
                    ex
            );
        }
    }

    private NotificationEntity createNotification(PaymentProcessedEvent event) {
        NotificationEntity entity = NotificationEntity.builder()
                .userId(event.userId())
                .email(event.email())
                .type(NotificationType.PAYMENT.name())
                .subject("Payment Confirmation #" + event.paymentId())
                .message(buildNotificationMessage(event))
                .status(NotificationStatus.PENDING)
                .build();

        log.info(
                "Saving payment notification. eventId={}, paymentId={}",
                event.eventId(),
                event.paymentId()
        );

        return notificationRepository.save(entity);
    }

    private void sendPaymentEmail(
            PaymentProcessedEvent event
    ) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();

        MimeMessageHelper helper =
                new MimeMessageHelper(message, true);

        helper.setFrom(properties.systemEmail());
        helper.setTo(event.email());
        helper.setSubject(
                "Payment Confirmation #" + event.paymentId()
        );

        helper.setText(buildPaymentEmail(event), true);

        log.info(
                "Sending payment confirmation email to {}",
                event.email()
        );

        mailSender.send(message);
    }

    private String buildPaymentEmail(PaymentProcessedEvent event) {
        String orderId =
                event.orderId() != null
                        ? event.orderId().toString()
                        : "N/A";

        String processedAt =
                LocalDateTime.ofInstant(
                                event.processedAt(),
                                ZoneId.systemDefault())
                        .format(DATE_FORMATTER);

        return """
                <html>
                <body>
                    <h2>Payment Confirmation</h2>
                
                    <p>Dear Customer,</p>
                
                    <p>
                        We are pleased to inform you that your payment has been successfully processed.
                    </p>
                
                    <h3>Payment Details</h3>
                
                    <ul>
                        <li><strong>Payment ID:</strong> %d</li>
                        <li><strong>Order ID:</strong> %s</li>
                        <li><strong>Amount:</strong> %s %s</li>
                        <li><strong>Status:</strong> %s</li>
                        <li><strong>Processed At:</strong> %s</li>
                    </ul>
                
                    <p>Thank you for your purchase.</p>
                
                    <p>
                        Best regards,<br>
                        E-Commerce Team
                    </p>
                </body>
                </html>
                """
                .formatted(
                        event.paymentId(),
                        orderId,
                        event.amount(),
                        event.currency(),
                        event.status(),
                        processedAt
                );
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
