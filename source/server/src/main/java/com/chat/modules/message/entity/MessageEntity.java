package com.chat.modules.message.entity;

import com.chat.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "messages")
public class MessageEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Column(name = "client_message_id", nullable = false, length = 128, updatable = false)
    private String clientMessageId;

    @Column(columnDefinition = "text")
    private String content;

    protected MessageEntity() {
    }

    public MessageEntity(UUID conversationId, UUID senderId, String clientMessageId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.content = content;
    }
}
