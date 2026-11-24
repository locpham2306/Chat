package com.chat.modules.user.dto.response;

import com.chat.modules.user.enums.UserStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// Chứa thông tin công khai và trạng thái của một user để BE trả lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String username;
    private String fullName;
    private UUID avatarFileId;
    private UserStatusEnum status;
    private Instant lastSeen;
}
