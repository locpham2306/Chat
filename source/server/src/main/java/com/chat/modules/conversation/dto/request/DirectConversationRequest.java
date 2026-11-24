package com.chat.modules.conversation.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

// Nhận ID người nhận để FE tìm cuộc hội thoại DIRECT với user đó.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DirectConversationRequest {
    @NotNull(message = "{validation.notNull}")
    private UUID recipientId;
}
