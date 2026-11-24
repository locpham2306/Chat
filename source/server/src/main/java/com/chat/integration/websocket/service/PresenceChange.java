package com.chat.integration.websocket.service;

import com.chat.integration.websocket.dto.PresenceChangedData;

import java.util.List;
import java.util.UUID;

// File này gom dữ liệu presence và danh sách user mà BE cần gửi event lên FE.
public record PresenceChange(PresenceChangedData data, List<UUID> audienceUserIds) {
}
