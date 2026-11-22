package com.chat.modules.message.dto.response;

import java.util.UUID;

// Chứa thông tin rút gọn của người gửi để hiển thị kèm tin nhắn.
public record MessageSenderResponse(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
