package com.chat.modules.conversation.entity;

import com.chat.common.base.BaseEntity;
import com.chat.modules.conversation.enums.MemberRoleEnum;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "conversation_members")
public class ConversationMemberEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private MemberRoleEnum role;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "left_at")
    private Instant leftAt;

    protected ConversationMemberEntity() {
    }

    public ConversationMemberEntity(UUID conversationId, UUID userId, MemberRoleEnum role, Instant joinedAt) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }
}
