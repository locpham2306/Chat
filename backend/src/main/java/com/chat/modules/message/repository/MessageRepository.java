package com.chat.modules.message.repository;

import com.chat.modules.conversation.repository.projection.ConversationCountProjection;
import com.chat.modules.message.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    Optional<MessageEntity> findBySenderIdAndClientMessageId(UUID senderId, String clientMessageId);

    @Query(MessageQueries.FIND_LATEST_BY_CONVERSATION_IDS)
    List<MessageEntity> findLatestByConversationIds(
            @Param("conversationIds") Collection<UUID> conversationIds);

    @Query(MessageQueries.COUNT_UNREAD_BY_CONVERSATION_IDS)
    List<ConversationCountProjection> countUnreadByConversationIds(
            @Param("userId") UUID userId,
            @Param("conversationIds") Collection<UUID> conversationIds);
}
