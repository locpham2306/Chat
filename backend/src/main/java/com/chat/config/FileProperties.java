package com.chat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

// Đọc thư mục lưu file và dung lượng tối đa từ app.file.
@ConfigurationProperties(prefix = "app.file")
public record FileProperties(Path storageRoot, DataSize maxSize) {

    // Chặn backend khởi động nếu cấu hình file bị thiếu hoặc sai.
    public FileProperties {
        if (storageRoot == null) {
            throw new IllegalArgumentException("File storage root is required");
        }
        if (maxSize == null || maxSize.toBytes() < 1) {
            throw new IllegalArgumentException("File max size must be positive");
        }
    }
}
