package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, UUID> {
    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId, UUID userId);

    Optional<ConversationMemberEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Query(ConversationMemberQueries.COUNT_ACTIVE_MEMBERSHIPS_FOR_CONVERSATION_AVATAR)
    long countActiveMembershipsForConversationAvatar(
            @Param("userId") UUID userId,
            @Param("avatarFileId") UUID avatarFileId);
}
