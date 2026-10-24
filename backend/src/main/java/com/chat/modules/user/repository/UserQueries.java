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

    private UserQueries() {
    }
}
