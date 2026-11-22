package com.chat.integration.websocket.dto;

import java.util.UUID;

// Chứa thông tin user rút gọn đi kèm các event realtime gửi lên FE.
public record RealtimeUserData(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
