package com.chat.modules.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

// Trả một trang lịch sử tin nhắn cùng cursor để FE tải trang tiếp theo.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryResponse {
    private List<MessageResponse> data;
    private UUID nextCursor;
    private boolean hasMore;
}
