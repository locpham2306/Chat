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

    public static final String FIND_ACTIVE_USER_IDS = """
            SELECT member.userId
            FROM ConversationMemberEntity member
            WHERE member.conversationId = :conversationId
              AND member.leftAt IS NULL
            """;

    public static final String FIND_ACTIVE_SHARED_CONVERSATION_USER_IDS = """
            SELECT DISTINCT other.userId
            FROM ConversationMemberEntity mine, ConversationMemberEntity other
            WHERE mine.userId = :userId
              AND mine.leftAt IS NULL
              AND other.conversationId = mine.conversationId
              AND other.leftAt IS NULL
              AND other.userId <> :userId
            """;

    private ConversationMemberQueries() {
    }
}
