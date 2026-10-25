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

    private ConversationQueries() {
    }
}
