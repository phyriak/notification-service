# Notification Service — Kafka & SMTP Integration

## Overview

The notification service consumes events from Apache Kafka (and exposes a REST API for newsletter signup), persists notification records in PostgreSQL, and delivers emails via SMTP (Brevo).

Payment processing and email delivery are decoupled: the payment service publishes events to Kafka; this service handles delivery asynchronously with status tracking and idempotency.

---

## Architecture

### Previous flow (synchronous)

```text
Order Service
    ↓
Payment Service
    ↓
Process Payment
    ↓
Send Email (SMTP)
    ↓
Customer
```

**Problems:** tight coupling, payment blocked on SMTP, poor fault tolerance, hard to scale notifications independently.

### Current flow (event-driven)

```text
Payment Service / REST Client
    ↓
Apache Kafka  (payment, newsletter.signup)
    ↓
NotificationKafkaListener
    ↓
NotificationIdempotencyService  (duplicate check)
    ↓
NotificationKafkaPublisher  (Spring application events)
    ↓
NotificationService
    ↓
Save notification (NEW)
    ↓
EmailPublisher + EmailTemplateService (Thymeleaf)
    ↓
SMTP (Brevo)
    ↓
Update status (SENT / FAILED)
```

### Package layout

| Package | Responsibility |
|---------|----------------|
| `notification_orchestrator` | Kafka listeners, event handlers, persistence, idempotency |
| `payment_consumer` | Payment DTOs and exceptions |
| `email` | SMTP sending and Thymeleaf templates |
| `newsletter` | REST API and signup request model |

---

## Kafka configuration

### Broker (UAT / Docker network)

```text
kafka:9092
```

### Topics

| Topic | Purpose |
|-------|---------|
| `payment` | Payment processed events |
| `newsletter.signup` | Newsletter signup events |

### Consumer group

```text
notification-group
```

### Consumer settings (UAT profile)

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    listener:
      ack-mode: record
```

`ack-mode: record` commits the offset only after the listener returns successfully. On failure the message is retried.

### Producer settings (payment-service)

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

Payment service example:

```java
kafkaTemplate.send(
    "payment",
    event.paymentId().toString(),
    event
);
```

Using `paymentId` as the Kafka message key keeps ordering per payment within a partition.

---

## Events

### PaymentProcessedEvent

Published to topic `payment` when a payment succeeds.

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | **Required.** Idempotency key |
| `paymentId` | Long | Payment identifier |
| `userId` | String | Customer user id |
| `orderId` | Long | Related order |
| `status` | PaymentStatus | e.g. `PAID` |
| `amount` | BigDecimal | Payment amount |
| `currency` | String | Currency code |
| `processedAt` | Instant | Processing timestamp |
| `email` | String | Recipient email |

### NewsLetterSignupRequest

Published to topic `newsletter.signup` or sent via REST.

| Field | Type | Description |
|-------|------|-------------|
| `eventId` | UUID | Idempotency key (auto-generated if missing) |
| `email` | String | **Required.** Valid email address |
| `userId` | String | Optional user id |
| `firstName` | String | Used in welcome email |
| `subject` | String | Notification subject |
| `message` | String | Stored notification message |
| `createdAt` | LocalDateTime | Signup timestamp |

### PaymentFailedEvent

Defined in `payment_consumer.dto` but not consumed yet. Reserved for future failure notifications.

---

## REST API

### Newsletter signup

```http
POST /api/newsletter/signup
Content-Type: application/json
```

Example body:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "customer@example.com",
  "firstName": "Anna",
  "userId": "42"
}
```

Validation: `@NotBlank` + `@Email` on `email`.

---

## Notification processing

### Status lifecycle

```text
NEW → SENT     (email delivered successfully)
NEW → FAILED   (SMTP or processing error)
```

| Status | Meaning |
|--------|---------|
| `NEW` | Notification persisted, email not yet confirmed sent |
| `SENT` | Email delivered successfully |
| `FAILED` | Delivery failed; `retryCount` incremented |

On Kafka retry after `FAILED`, the existing row is reused and send is attempted again.

### Processing steps

1. Kafka listener (or REST controller) receives the event.
2. `NotificationIdempotencyService` resolves `eventId` and skips if already `SENT`.
3. `NotificationKafkaPublisher` publishes a Spring application event.
4. `NotificationService` loads or creates a notification row (`NEW`).
5. `EmailPublisher` sends the HTML email.
6. Status updated to `SENT` or `FAILED`; on success, row added to `processed_events`.

---

## Idempotency

Duplicate Kafka deliveries are handled using two layers:

| Layer | Table | Purpose |
|-------|-------|---------|
| Kafka consumer | `processed_events` | Fast lookup — event successfully consumed |
| Business audit | `notifications` (`event_id` unique) | Full notification record and status |

### Rules

| Scenario | Behaviour |
|----------|-----------|
| First delivery | Insert `NEW` → send → `SENT` → save `processed_events` |
| Duplicate (already in `processed_events`) | Skipped at listener — no second email |
| Duplicate (`notifications` already `SENT`) | Skipped at listener — backward compatibility |
| Retry after `FAILED` | Not in `processed_events` → reload row → retry send |
| Concurrent duplicates | Unique constraints + reload existing row |
| Payment without `eventId` | Skipped with error log |
| Newsletter without `eventId` | UUID generated automatically |

### Implementation

- `NotificationIdempotencyService.shouldSkipProcessing()` — checks `processed_events` and `notifications` (`SENT`).
- `NotificationIdempotencyService.markAsProcessed()` — saves `ProcessedEvent` after successful email delivery.
- `NotificationRepository.findByEventId(UUID)` — loads existing rows for retries.
- Unique constraint `uq_event_id` on `notifications.event_id`.

`processed_events` is written **only after success** so Kafka can still retry when status is `FAILED`.

---

## Email delivery

### Components

| Class | Role |
|-------|------|
| `EmailPublisher` | Builds and sends MIME messages |
| `EmailTemplateService` | Renders Thymeleaf HTML templates |
| `EmailProperties` | Sender email and retry config |

### Templates

| Template | Used for |
|----------|----------|
| `templates/emails/payment-confirmation.html` | Payment confirmation |
| `templates/emails/signup-welcome.html` | Newsletter welcome |

### Configuration

```yaml
spring:
  mail:
    host: ${EMAIL_HOST}
    port: ${EMAIL_PORT}
    username: ${EMAIL_USER}
    password: ${EMAIL_PASSWORD}
    protocol: smtp

email-notification:
  system-email: ${SENDER_EMAIL}
  retry-count: 3
```

### Environment variables

```text
EMAIL_HOST=smtp-relay.brevo.com
EMAIL_PORT=587
EMAIL_USER=<smtp-login>
EMAIL_PASSWORD=<smtp-password>
SENDER_EMAIL=<verified-sender@domain.com>
```

### SMTP login vs sender address

| Concept | Example | Usage |
|---------|---------|-------|
| SMTP login | `aeb471001@smtp-brevo.com` | Authentication only |
| Sender (`From`) | `ecommerce2137@op.pl` | `helper.setFrom(...)` — must be verified in Brevo |

UAT SMTP settings:

```yaml
spring:
  mail:
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## Why Brevo?

The previous solution used a personal mailbox (Onet), which was eventually blocked.

| Problem (personal SMTP) | Brevo benefit |
|-------------------------|---------------|
| Account suspension risk | Transactional email infrastructure |
| Security / anomaly blocks | Dedicated SMTP credentials |
| No delivery monitoring | Delivery tracking and relay |
| Not designed for apps | API + SMTP support |

---

## Brevo security

SMTP access can be restricted to authorized IP addresses in the Brevo dashboard.

| Environment | IP |
|-------------|-----|
| Production (Hetzner VPS) | `178.105.32.216` |
| Local development | Your public IP |

---

## Database

### `notifications`

Stores notification records (Liquibase migration `002-create-notification-events.yaml`).

Key columns: `event_id` (UUID, unique), `email`, `type`, `subject`, `message`, `status`, `retry_count`, `created_at`, `updated_at`.

### `processed_events`

Kafka consumer idempotency log (Liquibase migration `001-create-processed-events.yaml`).

| Column | Description |
|--------|-------------|
| `event_id` | Primary key — same UUID as the Kafka event |
| `processed_at` | Timestamp when email was successfully delivered |

Written by `NotificationIdempotencyService.markAsProcessed()` after status becomes `SENT`.

UAT uses `ddl-auto: validate` with Liquibase enabled.

---

## Monitoring

Spring Boot Actuator endpoints:

```text
/actuator/health
/actuator/info
/actuator/prometheus
```

Health checks include mail connectivity when configured.

---

## Local development

Profile: `local`

```text
SPRING_PROFILES_ACTIVE=local
```

- H2 in-memory database
- Kafka auto-startup disabled by default (`spring.kafka.listener.auto-startup: false`)
- Mail debug enabled in local profile

Server port: `8089`

---

## Future improvements

* Dead Letter Queue (DLQ) for poison Kafka messages
* Automatic retry scheduler using `email-notification.retry-count`
* `PaymentFailedEvent` consumer and failure email template
* Domain authentication (SPF / DKIM / DMARC)
* SMS and push notification channels
* Notification dashboard
