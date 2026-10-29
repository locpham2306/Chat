package com.chat.modules.message.dto.response;

import java.util.List;
import java.util.UUID;

public record MessageHistoryResponse(
        List<MessageResponse> data,
        UUID nextCursor,
        boolean hasMore) {
}
