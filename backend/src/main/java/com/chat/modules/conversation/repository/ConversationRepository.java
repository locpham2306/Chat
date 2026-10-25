package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Query(ConversationQueries.FIND_DIRECT_BETWEEN_USERS)
    Optional<ConversationEntity> findDirectBetweenUsers(
            @Param("type") ConversationTypeEnum type,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId);
}
