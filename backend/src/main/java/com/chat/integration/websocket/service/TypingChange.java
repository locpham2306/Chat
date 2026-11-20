package com.chat.integration.websocket.service;

import com.chat.integration.websocket.dto.TypingData;

import java.util.List;
import java.util.UUID;

// File này gom dữ liệu typing và danh sách user mà BE cần gửi event lên FE.
public record TypingChange(TypingData data, List<UUID> audienceUserIds) {
}
