package com.chat.modules.message.repository;

public final class MessageQueries {
    public static final String FIND_LATEST_BY_CONVERSATION_IDS = """
            SELECT message
            FROM MessageEntity message
            WHERE message.conversationId IN :conversationIds
              AND message.createdAt = (
                  SELECT MAX(candidate.createdAt)
                  FROM MessageEntity candidate
                  WHERE candidate.conversationId = message.conversationId
              )
            ORDER BY message.conversationId ASC, message.id DESC
            """;

    public static final String COUNT_UNREAD_BY_CONVERSATION_IDS = """
            SELECT message.conversationId AS conversationId, COUNT(message) AS total
            FROM MessageEntity message
            JOIN ConversationMemberEntity member
              ON member.conversationId = message.conversationId
            LEFT JOIN ConversationReadStateEntity readState
              ON readState.memberId = member.id
            LEFT JOIN MessageEntity readMessage
              ON readMessage.id = readState.lastReadMessageId
            WHERE member.userId = :userId
              AND member.leftAt IS NULL
              AND message.conversationId IN :conversationIds
              AND (readMessage.id IS NULL
                   OR message.createdAt > readMessage.createdAt
                   OR (message.createdAt = readMessage.createdAt AND message.id > readMessage.id))
            GROUP BY message.conversationId
            """;

    public static final String FIND_HISTORY_BEFORE = """
            SELECT message
            FROM MessageEntity message
            WHERE message.conversationId = :conversationId
              AND (message.createdAt < :beforeCreatedAt
                   OR (message.createdAt = :beforeCreatedAt AND message.id < :beforeId))
            ORDER BY message.createdAt DESC, message.id DESC
            """;

    private MessageQueries() {
    }
}
