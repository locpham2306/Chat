package com.chat.modules.file.entity;

import com.chat.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "files")
public class FileEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "uploader_id", nullable = false, updatable = false)
    private UUID uploaderId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long size;

    protected FileEntity() {
    }

    public FileEntity(UUID uploaderId, String name, String storageKey, String contentType, long size) {
        this.uploaderId = uploaderId;
        this.name = name;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.size = size;
    }

    public void bindToConversation(UUID conversationId) {
        if (conversationId == null || (this.conversationId != null && !this.conversationId.equals(conversationId))) {
            throw new IllegalArgumentException("File cannot be moved to another conversation");
        }
        this.conversationId = conversationId;
    }
}
