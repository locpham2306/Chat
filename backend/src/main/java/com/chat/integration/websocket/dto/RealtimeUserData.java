package com.chat.integration.websocket.dto;

import java.util.UUID;

// File này định nghĩa thông tin user rút gọn BE gửi lên FE trong các event realtime.
// FE nhận đủ tên/avatar nên không phải gọi thêm API.
public record RealtimeUserData(
        UUID id,
        String fullName,
        UUID avatarFileId) {
}
