package com.chat.modules.conversation.entity;

import com.chat.common.base.BaseEntity;
import com.chat.modules.conversation.enums.ConversationTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @Setter
    private ConversationTypeEnum type;

    @Column(length = 100)
    @Setter
    private String name;

    @Column(name = "owner_id")
    @Setter
    private UUID ownerId;

    @Column(name = "avatar_file_id")
    @Setter
    private UUID avatarFileId;

    @Column(name = "direct_user_low_id", updatable = false)
    @Setter
    private UUID directUserLowId;

    @Column(name = "direct_user_high_id", updatable = false)
    @Setter
    private UUID directUserHighId;

    public ConversationEntity() {
    }
}
