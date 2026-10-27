package com.chat.modules.user.repository;

public final class UserQueries {
    public static final String SEARCH_PUBLIC_PROFILES = """
            SELECT user
            FROM UserEntity user
            WHERE user.id <> :currentUserId
              AND (:keyword = ''
                OR LOCATE(LOWER(:keyword), LOWER(user.username)) > 0
                OR LOCATE(LOWER(:keyword), LOWER(user.fullName)) > 0)
            """;

    public static final String SEARCH_AVAILABLE_GROUP_MEMBERS = """
            SELECT user
            FROM UserEntity user
            WHERE user.id <> :currentUserId
              AND NOT EXISTS (
                SELECT member.id
                FROM ConversationMemberEntity member
                WHERE member.conversationId = :conversationId
                  AND member.userId = user.id
                  AND member.leftAt IS NULL)
              AND (:keyword = ''
                OR LOCATE(LOWER(:keyword), LOWER(user.username)) > 0
                OR LOCATE(LOWER(:keyword), LOWER(user.fullName)) > 0)
            """;

    private UserQueries() {
    }
}
