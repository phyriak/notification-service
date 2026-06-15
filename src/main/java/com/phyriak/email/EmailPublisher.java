package com.phyriak.email;

import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.payment_consumer.dto.PaymentProcessedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailPublisher {
    private final JavaMailSender mailSender;
    private final EmailProperties properties;
    private final EmailTemplateService templateService;


    public void sendNewsletterSignup(
            NewsLetterSignupRequest request
    ) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        log.info("Sending welcome email to: {}", request.getEmail());
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(properties.systemEmail());
        helper.setTo(request.getEmail());
        helper.setSubject("Welcome " + request.getFirstName() +" !");
        helper.setText(templateService.buildWelcomeEmail(request), true);
        mailSender.send(message);
    }

    public void sendPaymentEmail(
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

        helper.setText(templateService.buildPaymentConfirmationEmail(event), true);

        log.info(
                "Sending payment confirmation email to {}",
                event.email()
        );

        mailSender.send(message);
    }
}
