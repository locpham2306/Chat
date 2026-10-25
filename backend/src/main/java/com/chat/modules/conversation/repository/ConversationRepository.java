package com.chat.modules.conversation.repository;

import com.chat.modules.conversation.entity.ConversationEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

    @Query(ConversationQueries.FIND_DIRECT_BETWEEN_USERS)
    Optional<ConversationEntity> findDirectBetweenUsers(
            @Param("type") ConversationTypeEnum type,
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId);

    @Query(value = ConversationQueries.FIND_ACTIVE_FOR_USER,
            countQuery = ConversationQueries.COUNT_ACTIVE_FOR_USER)
    Page<ConversationEntity> findActiveForUser(@Param("userId") UUID userId, Pageable pageable);
}
