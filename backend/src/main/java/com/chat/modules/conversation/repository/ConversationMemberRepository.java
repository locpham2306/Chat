package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, UUID> {
    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId, UUID userId);
}
