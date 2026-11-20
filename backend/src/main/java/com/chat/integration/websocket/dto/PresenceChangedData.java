package com.chat.integration.websocket.dto;

import com.chat.modules.user.enums.UserStatusEnum;

import java.time.Instant;
import java.util.UUID;

// File này định nghĩa dữ liệu BE gửi lên FE để báo một user vừa ONLINE hoặc OFFLINE.
// lastSeen là lần hoạt động cuối; khi ONLINE có thể vẫn là giá trị cũ trong DB.
public record PresenceChangedData(
        UUID userId,
        RealtimeUserData user,
        UserStatusEnum status,
        Instant lastSeen) {
}
