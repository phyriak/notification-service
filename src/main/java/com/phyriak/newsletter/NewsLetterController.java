package com.phyriak.newsletter;

import com.phyriak.newsletter.model.NewsLetterSignupRequest;
import com.phyriak.notification_orchestrator.service.NotificationIdempotencyService;
import com.phyriak.notification_orchestrator.service.NotificationKafkaPublisher;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newsletter")
@Slf4j
public class NewsLetterController {

    private final NotificationKafkaPublisher newsletterService;
    private final NotificationIdempotencyService idempotencyService;

    public NewsLetterController(
            NotificationKafkaPublisher newsletterService,
            NotificationIdempotencyService idempotencyService
    ) {
        this.newsletterService = newsletterService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody NewsLetterSignupRequest request) {
        request.setEventId(idempotencyService.resolveEventId(request.getEventId()));

        if (idempotencyService.shouldSkipProcessing(request.getEventId())) {
            return ResponseEntity.ok("Already signed up!");
        }

        newsletterService.signup(request);
        return ResponseEntity.ok("Signed up!");
    }
}
