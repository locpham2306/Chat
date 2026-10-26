package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationMemberEntity;
import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, UUID> {
    boolean existsByConversationIdAndUserIdAndLeftAtIsNull(UUID conversationId, UUID userId);

    long countByConversationIdAndLeftAtIsNull(UUID conversationId);

    Optional<ConversationMemberEntity> findByConversationIdAndUserId(UUID conversationId, UUID userId);

    Optional<ConversationMemberEntity> findByConversationIdAndUserIdAndLeftAtIsNull(
            UUID conversationId, UUID userId);

    @Query(ConversationMemberQueries.COUNT_ACTIVE_MEMBERSHIPS_FOR_CONVERSATION_AVATAR)
    long countActiveMembershipsForConversationAvatar(
            @Param("userId") UUID userId,
            @Param("avatarFileId") UUID avatarFileId);

    @Query(ConversationMemberQueries.COUNT_ACTIVE_MEMBERS_BY_CONVERSATIONS)
    List<ConversationCountProjection> countActiveMembersByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds);
}
