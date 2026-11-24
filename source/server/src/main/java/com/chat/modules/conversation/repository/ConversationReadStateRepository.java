package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationReadStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationReadStateRepository
        extends JpaRepository<ConversationReadStateEntity, UUID> {
}
