package com.chat.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins, long maxAgeSeconds) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream().map(String::trim).filter(origin -> !origin.isEmpty()).distinct().toList();
        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("CORS allowed origins must contain explicit origins");
        }
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException("CORS max age must not be negative");
        }
    }
}
