package com.chat.modules.message.entity;

import com.chat.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "message_attachments")
public class MessageAttachmentEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @Column(nullable = false)
    private int position;

    protected MessageAttachmentEntity() {
    }

    public MessageAttachmentEntity(UUID messageId, UUID fileId, int position) {
        this.messageId = messageId;
        this.fileId = fileId;
        this.position = position;
    }
}
