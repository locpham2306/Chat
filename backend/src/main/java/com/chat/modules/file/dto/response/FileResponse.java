package com.chat.modules.file.dto.response;

import java.util.UUID;

// Chứa metadata và đường dẫn tải của một file để BE trả lên FE.
public record FileResponse(
        UUID id,
        String name,
        String contentType,
        long size,
        String downloadUrl) {
}
