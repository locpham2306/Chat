package com.chat.modules.message.repository;

import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import com.chat.modules.message.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    Optional<MessageEntity> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId);

    List<MessageEntity> findByConversationIdOrderByCreatedAtDescIdDesc(
            UUID conversationId, Pageable pageable);

    @Query(MessageQueries.FIND_HISTORY_BEFORE)
    List<MessageEntity> findHistoryBefore(
            @Param("conversationId") UUID conversationId,
            @Param("beforeCreatedAt") Instant beforeCreatedAt,
            @Param("beforeId") UUID beforeId,
            Pageable pageable);

    @Query(MessageQueries.FIND_LATEST_BY_CONVERSATION_IDS)
    List<MessageEntity> findLatestByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds);

    @Query(MessageQueries.COUNT_UNREAD_BY_CONVERSATION_IDS)
    List<ConversationCountProjection> countUnreadByConversationIds(
            @Param("userId") UUID userId,
            @Param("conversationIds") Collection<UUID> conversationIds);
}
