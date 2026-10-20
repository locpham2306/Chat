package com.chat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class ChatSchemaMigrationTests {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withCommand("--log-bin-trust-function-creators=1");

    @Test
    void upgradesPopulatedV3WithoutLosingLinksAndEnforcesNormalizedConstraints() {
        var config = Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        config.target("3").load().migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(
                MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()));
        jdbc.update("""
                INSERT INTO users (id, username, password_hash, full_name, created_at, updated_at)
                VALUES (UNHEX(LPAD('1',32,'0')), 'migration', 'hash', 'An', NOW(6), NOW(6))
                """);
        for (String id : new String[]{"2", "3"}) {
            jdbc.update("""
                    INSERT INTO conversations (id, type, name, owner_id, created_at, updated_at)
                    VALUES (UNHEX(LPAD(?,32,'0')), 'GROUP', 'Group', UNHEX(LPAD('1',32,'0')), NOW(6), NOW(6))
                    """, id);
        }
        jdbc.update("""
                INSERT INTO messages (id, conversation_id, sender_id, client_message_id, content, created_at, updated_at)
                VALUES (UNHEX(LPAD('4',32,'0')), UNHEX(LPAD('2',32,'0')), UNHEX(LPAD('1',32,'0')),
                        'client', 'Xin chào', NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO conversation_members
                    (id, conversation_id, user_id, role, joined_at, last_read_message_id, created_at, updated_at)
                VALUES (UNHEX(LPAD('5',32,'0')), UNHEX(LPAD('2',32,'0')), UNHEX(LPAD('1',32,'0')),
                        'OWNER', NOW(6), UNHEX(LPAD('4',32,'0')), NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO files (id, uploader_id, conversation_id, name, storage_key, content_type, size, created_at, updated_at)
                VALUES (UNHEX(LPAD('6',32,'0')), UNHEX(LPAD('1',32,'0')), UNHEX(LPAD('2',32,'0')),
                        'a.txt', 'a.txt', 'text/plain', 1, NOW(6), NOW(6))
                """);
        jdbc.update("""
                INSERT INTO message_attachments (id, conversation_id, message_id, file_id, position, created_at, updated_at)
                VALUES (UNHEX(LPAD('7',32,'0')), UNHEX(LPAD('2',32,'0')), UNHEX(LPAD('4',32,'0')),
                        UNHEX(LPAD('6',32,'0')), 0, NOW(6), NOW(6))
                """);
        var audit = jdbc.queryForMap("SELECT created_at, updated_at FROM conversation_members");
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword()).load().migrate();
        assertThat(jdbc.queryForMap("SELECT created_at, updated_at FROM conversation_read_states")).isEqualTo(audit);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM conversation_read_states r
                JOIN conversation_members cm ON cm.id = r.member_id
                JOIN messages m ON m.id = r.last_read_message_id
                JOIN message_attachments a ON a.message_id = m.id
                JOIN files f ON f.id = a.file_id
                WHERE cm.conversation_id = m.conversation_id AND f.conversation_id = m.conversation_id
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
                AND ((table_name = 'message_attachments' AND column_name = 'conversation_id')
                  OR (table_name = 'conversation_members' AND column_name = 'last_read_message_id'))
                """, Integer.class)).isZero();
        // Parent updates must not bypass the cross-conversation checks.
        for (String table : new String[]{"messages", "conversation_members", "files"}) {
            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE " + table + " SET conversation_id = UNHEX(LPAD('3',32,'0'))"))
                    .hasRootCauseInstanceOf(java.sql.SQLException.class);
        }
        jdbc.update("""
                INSERT INTO messages (id, conversation_id, sender_id, client_message_id, created_at, updated_at)
                VALUES (UNHEX(LPAD('8',32,'0')), UNHEX(LPAD('3',32,'0')), UNHEX(LPAD('1',32,'0')),
                        'other', NOW(6), NOW(6))
                """);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE conversation_read_states SET last_read_message_id = UNHEX(LPAD('8',32,'0'))
                """)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE message_attachments SET message_id = UNHEX(LPAD('8',32,'0'))
                """)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        // Valid updates still work.
        assertThat(jdbc.update("UPDATE message_attachments SET position = 1")).isEqualTo(1);
        assertThat(jdbc.update("UPDATE conversation_read_states SET updated_at = NOW(6)")).isEqualTo(1);
    }
}
