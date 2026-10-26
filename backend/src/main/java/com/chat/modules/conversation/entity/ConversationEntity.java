package com.chat.modules.conversation.entity;

import com.chat.common.base.BaseEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "conversations")
public class ConversationEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10, updatable = false)
    private ConversationTypeEnum type;

    @Column(length = 100)
    private String name;

    @Column(name = "owner_id")
    private UUID ownerId;

    @Column(name = "avatar_file_id")
    private UUID avatarFileId;

    @Column(name = "direct_user_low_id", updatable = false)
    private UUID directUserLowId;

    @Column(name = "direct_user_high_id", updatable = false)
    private UUID directUserHighId;

    protected ConversationEntity() {
    }

    public static ConversationEntity direct(UUID firstUserId, UUID secondUserId) {
        Objects.requireNonNull(firstUserId);
        Objects.requireNonNull(secondUserId);
        if (firstUserId.equals(secondUserId)) {
            throw new IllegalArgumentException("A direct conversation requires two different users");
        }
        var conversation = new ConversationEntity();
        conversation.type = ConversationTypeEnum.DIRECT;
        // Canonical UUID strings sort in the same unsigned byte order as MySQL BINARY(16).
        boolean firstIsLow = firstUserId.toString().compareTo(secondUserId.toString()) < 0;
        conversation.directUserLowId = firstIsLow ? firstUserId : secondUserId;
        conversation.directUserHighId = firstIsLow ? secondUserId : firstUserId;
        return conversation;
    }

    public static ConversationEntity group(UUID ownerId, String name, UUID avatarFileId) {
        var conversation = new ConversationEntity();
        conversation.type = ConversationTypeEnum.GROUP;
        conversation.ownerId = Objects.requireNonNull(ownerId);
        conversation.name = Objects.requireNonNull(name);
        conversation.avatarFileId = avatarFileId;
        return conversation;
    }

    public void updateGroup(String name, UUID avatarFileId) {
        if (type != ConversationTypeEnum.GROUP) {
            throw new IllegalStateException("Only group conversations can be updated");
        }
        if (name == null || name.isBlank() || name.length() > 100) {
            throw new IllegalArgumentException("Invalid group name");
        }
        this.name = name;
        this.avatarFileId = avatarFileId;
    }
}
