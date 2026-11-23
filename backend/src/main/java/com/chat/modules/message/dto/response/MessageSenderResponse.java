package com.chat.modules.message.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Chứa thông tin rút gọn của người gửi để hiển thị kèm tin nhắn.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSenderResponse {
    private UUID id;
    private String fullName;
    private UUID avatarFileId;
}
