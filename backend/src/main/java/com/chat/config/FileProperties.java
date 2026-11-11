package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "app.file")
public record FileProperties(
        Path storageRoot,
        DataSize maxSize) {

    public FileProperties {
        if (storageRoot == null) {
            throw new IllegalArgumentException("File storage root is required");
        }
        if (maxSize == null || maxSize.toBytes() < 1) {
            throw new IllegalArgumentException("File max size must be positive");
        }
    }
}
