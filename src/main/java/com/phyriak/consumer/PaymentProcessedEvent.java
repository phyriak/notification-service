package com.phyriak.consumer;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID eventId,
        Long paymentId,
        Long userId,
        Long orderId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        Instant processedAt,
        String email
) {
}