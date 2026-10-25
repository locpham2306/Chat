package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "app.file")
public record FileProperties(
        Path storageRoot,
        DataSize maxSize,
        List<String> allowedContentTypes) {

    public FileProperties {
        if (storageRoot == null) {
            throw new IllegalArgumentException("File storage root is required");
        }
        if (maxSize == null || maxSize.toBytes() < 1) {
            throw new IllegalArgumentException("File max size must be positive");
        }
        allowedContentTypes = allowedContentTypes == null
                ? List.of()
                : allowedContentTypes.stream()
                        .map(value -> value.trim().toLowerCase(Locale.ROOT))
                        .filter(value -> !value.isEmpty())
                        .distinct()
                        .toList();
        if (allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("At least one file content type must be allowed");
        }
    }

    public boolean allows(String contentType) {
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return allowedContentTypes.stream().anyMatch(allowed ->
                allowed.equals(normalized)
                        || (allowed.endsWith("/*")
                        && normalized.startsWith(allowed.substring(0, allowed.length() - 1))));
    }
}
