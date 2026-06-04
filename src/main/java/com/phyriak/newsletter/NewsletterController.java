package com.phyriak.newsletter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/newsletter")
@Slf4j
public class NewsletterController {

    private final NotificationService newsletterService;

    public NewsletterController(NotificationService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestParam String email, @RequestParam Long userId) {
        newsletterService.signup(new NewsLetterSignupEvent(email, userId, LocalDateTime.now()));
        return ResponseEntity.ok("Signed up!");
    }
}
