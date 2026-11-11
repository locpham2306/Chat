package com.chat.modules.conversation.entity;

import com.chat.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "conversation_read_states")
public class ConversationReadStateEntity extends BaseEntity {
    @Id
    @Column(name = "member_id", nullable = false, updatable = false)
    private UUID memberId;

    @Column(name = "last_read_message_id", nullable = false)
    private UUID lastReadMessageId;

    protected ConversationReadStateEntity() {
    }

    public ConversationReadStateEntity(UUID memberId, UUID lastReadMessageId) {
        this.memberId = memberId;
        this.lastReadMessageId = lastReadMessageId;
    }

    public void advanceTo(UUID lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }
}
