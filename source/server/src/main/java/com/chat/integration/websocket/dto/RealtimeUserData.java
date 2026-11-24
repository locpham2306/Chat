package com.chat.integration.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Chứa thông tin user rút gọn đi kèm các event realtime gửi lên FE.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeUserData {
    private UUID id;
    private String fullName;
    private UUID avatarFileId;
}
