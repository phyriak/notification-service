package com.phyriak.newsletter.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
public class NewsLetterSignupRequest {

    private UUID eventId;
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email format required")
    private String email;
    private String userId;
    private String firstName;
    private String subject;
    private String message;
    private LocalDateTime createdAt;

    public NewsLetterSignupRequest() {
    }


    public NewsLetterSignupRequest(String email, String userId) {
        this.eventId = UUID.randomUUID();
        this.email = email;
        this.userId = userId;
        this.subject = "Newsletter signup";
        this.createdAt = LocalDateTime.now();
    }
}
