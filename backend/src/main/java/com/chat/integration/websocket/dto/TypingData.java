package com.chat.integration.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// Báo realtime cho FE rằng một người trong hội thoại đang nhập hoặc đã dừng nhập.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TypingData {
    private UUID conversationId;
    private UUID userId;
    private RealtimeUserData user;

    @Getter(onMethod_ = @JsonProperty("isTyping"))
    private boolean typing;
}
