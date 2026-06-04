package com.phyriak.newsletter;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class NewsLetterSignupEvent {

    private String email;
    private Long userId;
    private LocalDateTime createdAt;

    public NewsLetterSignupEvent() {}

    public NewsLetterSignupEvent(String email, Long userId, LocalDateTime createdAt) {
        this.email = email;
        this.userId = userId;
        this.createdAt = createdAt;
    }

}
