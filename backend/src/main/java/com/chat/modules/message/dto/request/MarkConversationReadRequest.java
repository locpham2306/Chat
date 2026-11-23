package com.chat.modules.message.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

// Nhận ID tin nhắn cuối cùng user đã đọc trong một cuộc hội thoại.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class MarkConversationReadRequest {
    @NotNull(message = "{validation.notNull}")
    private UUID lastReadMessageId;
}
