package com.chat.modules.user.dto.response;

import com.chat.modules.user.enums.UserStatusEnum;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin công khai và trạng thái của một user để BE trả lên FE.
public record UserResponse(
        UUID id,
        String username,
        String fullName,
        UUID avatarFileId,
        UserStatusEnum status,
        Instant lastSeen) {
}
