package com.chat.modules.file.dto.response;

import java.util.UUID;

public record FileResponse(
        UUID id,
        String name,
        String contentType,
        long size,
        String downloadUrl) {
}
