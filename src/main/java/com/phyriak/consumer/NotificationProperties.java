package com.phyriak.consumer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        String systemEmail,
        Integer retryCount
) {
}
