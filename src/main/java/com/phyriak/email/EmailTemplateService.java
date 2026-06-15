package com.phyriak.email;

import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.payment_consumer.dto.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SpringTemplateEngine templateEngine;

    public String buildPaymentConfirmationEmail(
            PaymentProcessedEvent event
    ) {
        String orderId =
                event.orderId() != null
                        ? event.orderId().toString()
                        : "N/A";

        String processedAt =
                LocalDateTime.ofInstant(
                                event.processedAt(),
                                ZoneId.systemDefault())
                        .format(DATE_FORMATTER);

        Context context = new Context();

        context.setVariable("paymentId", event.paymentId());
        context.setVariable("orderId", orderId);
        context.setVariable("amount", event.amount());
        context.setVariable("currency", event.currency());
        context.setVariable("status", event.status());
        context.setVariable("processedAt", processedAt);

        return templateEngine.process(
                "emails/payment-confirmation",
                context
        );
    }

    public String buildWelcomeEmail(NewsLetterSignupRequest request) {
        Context context = new Context();
        context.setVariable("firstName", request.getFirstName());
        context.setVariable("email", request.getEmail());

        return templateEngine.process("emails/signup-welcome.html", context);
    }
}
