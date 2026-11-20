package com.chat.integration.websocket.dto;

import java.util.UUID;

// File này định nghĩa dữ liệu FE gửi xuống BE khi bắt đầu hoặc dừng nhập tin.
public record TypingFrameData(UUID conversationId, Boolean isTyping) {
}

// TypingFrameData
// FE của An gửi xuống BE
// chỉ chứa conversationId và isTyping

// TypingData
// BE gửi lên FE của thành viên khác
// có thêm thông tin An để hiển thị “An đang nhập...”
