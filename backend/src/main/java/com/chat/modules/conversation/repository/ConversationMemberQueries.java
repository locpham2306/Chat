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

    public static final String COUNT_ACTIVE_MEMBERS_BY_CONVERSATIONS = """
            SELECT member.conversationId AS conversationId, COUNT(member) AS total
            FROM ConversationMemberEntity member
            WHERE member.conversationId IN :conversationIds
              AND member.leftAt IS NULL
            GROUP BY member.conversationId
            """;

    private ConversationMemberQueries() {
    }
}
