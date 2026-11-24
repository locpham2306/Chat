package com.chat.modules.message.repository;

import com.chat.modules.message.entity.MessageAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachmentEntity, UUID> {
    List<MessageAttachmentEntity> findByMessageIdOrderByPositionAsc(UUID messageId);

    List<MessageAttachmentEntity> findByMessageIdInOrderByMessageIdAscPositionAsc(
            Collection<UUID> messageIds);
}
