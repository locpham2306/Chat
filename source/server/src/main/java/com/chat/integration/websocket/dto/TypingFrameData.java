package com.chat.integration.websocket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

// Nhận trạng thái đang nhập mà FE gửi xuống BE qua WebSocket.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class TypingFrameData {
    private UUID conversationId;

    @Getter(onMethod_ = @JsonProperty("isTyping"))
    @Setter(onMethod_ = @JsonProperty("isTyping"))
    private Boolean typing;
}
