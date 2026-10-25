package com.chat.modules.conversation.repository;

public final class ConversationMemberQueries {
    public static final String COUNT_ACTIVE_MEMBERSHIPS_FOR_CONVERSATION_AVATAR = """
            SELECT COUNT(member)
            FROM ConversationMemberEntity member, ConversationEntity conversation
            WHERE member.conversationId = conversation.id
              AND member.userId = :userId
              AND member.leftAt IS NULL
              AND conversation.avatarFileId = :avatarFileId
            """;

    private ConversationMemberQueries() {
    }
}
