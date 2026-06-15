package com.phyriak.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "email-notification")
public record EmailProperties(
        String systemEmail,
        Integer retryCount
) {
}
