package com.chat.modules.message.dto.response;

import java.util.List;
import java.util.UUID;

// Trả một trang lịch sử tin nhắn cùng cursor để FE tải trang tiếp theo.
public record MessageHistoryResponse(
        List<MessageResponse> data,
        UUID nextCursor,
        boolean hasMore) {
}
