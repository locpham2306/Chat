package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// Đọc nhóm cấu hình app.cors.
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins, long maxAgeSeconds) {

    // Làm sạch danh sách origin và chặn cấu hình CORS không an toàn.
    public CorsProperties {
        List<String> cleanedOrigins = new ArrayList<>();
        if (allowedOrigins != null) {
            Set<String> distinctOrigins = new LinkedHashSet<>();
            for (String origin : allowedOrigins) {
                String cleanedOrigin = origin.trim();
                if (!cleanedOrigin.isEmpty()) {
                    distinctOrigins.add(cleanedOrigin);
                }
            }
            cleanedOrigins.addAll(distinctOrigins);
        }
        allowedOrigins = List.copyOf(cleanedOrigins);

        if (allowedOrigins.isEmpty() || allowedOrigins.contains("*")) {
            throw new IllegalArgumentException("CORS allowed origins must contain explicit origins");
        }
        if (maxAgeSeconds < 0) {
            throw new IllegalArgumentException("CORS max age must not be negative");
        }
    }
}
