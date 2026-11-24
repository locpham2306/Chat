package com.chat.modules.conversation.repository;

public final class ConversationQueries {
    public static final String FIND_DIRECT_BETWEEN_USERS = """
            SELECT conversation
            FROM ConversationEntity conversation
            WHERE conversation.type = :type
              AND ((conversation.directUserLowId = :firstUserId
                    AND conversation.directUserHighId = :secondUserId)
                OR (conversation.directUserLowId = :secondUserId
                    AND conversation.directUserHighId = :firstUserId))
            """;

    public static final String FIND_ACTIVE_FOR_USER = """
            SELECT conversation
            FROM ConversationEntity conversation, ConversationMemberEntity member
            WHERE member.conversationId = conversation.id
              AND member.userId = :userId
              AND member.leftAt IS NULL
            ORDER BY COALESCE(
                (SELECT MAX(message.createdAt)
                 FROM MessageEntity message
                 WHERE message.conversationId = conversation.id),
                conversation.createdAt) DESC,
                conversation.id DESC
            """;

    public static final String COUNT_ACTIVE_FOR_USER = """
            SELECT COUNT(conversation)
            FROM ConversationEntity conversation, ConversationMemberEntity member
            WHERE member.conversationId = conversation.id
              AND member.userId = :userId
              AND member.leftAt IS NULL
            """;

    public static final String FIND_BY_ID_FOR_UPDATE = """
            SELECT conversation
            FROM ConversationEntity conversation
            WHERE conversation.id = :id
            """;

    private ConversationQueries() {
    }
}
