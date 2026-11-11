-- Local development data only. Do not run against shared or production databases.
-- All seeded users use the password: 123456
SET NAMES utf8mb4;
SET time_zone = '+00:00';

START TRANSACTION;

SET @seed_now = UTC_TIMESTAMP(6);
SET @seed_password_hash = '$2a$10$38H4AA26cWEf7HidNxcqWuuhRtstS5V2hLFvgFeqiwXzXdK5mqh/y';

-- Users
INSERT INTO users (
    id, username, password_hash, full_name, avatar_file_id, last_seen, created_at, updated_at
) VALUES
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'seed_an',
     @seed_password_hash, 'Nguyễn Văn An', NULL, DATE_SUB(@seed_now, INTERVAL 2 MINUTE),
     DATE_SUB(@seed_now, INTERVAL 30 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), 'seed_binh',
     @seed_password_hash, 'Trần Gia Bình', NULL, DATE_SUB(@seed_now, INTERVAL 15 MINUTE),
     DATE_SUB(@seed_now, INTERVAL 25 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000103'), 'seed_chi',
     @seed_password_hash, 'Lê Minh Chi', NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY),
     DATE_SUB(@seed_now, INTERVAL 20 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000104'), 'seed_dung',
     @seed_password_hash, 'Phạm Hoàng Dũng', NULL, NULL,
     DATE_SUB(@seed_now, INTERVAL 15 DAY), @seed_now)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    full_name = VALUES(full_name),
    last_seen = VALUES(last_seen),
    updated_at = VALUES(updated_at);

-- One DIRECT conversation and two GROUP conversations.
INSERT INTO conversations (
    id, type, name, owner_id, avatar_file_id, direct_user_low_id, direct_user_high_id,
    created_at, updated_at
) VALUES
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000201'), 'DIRECT', NULL, NULL, NULL,
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000102'),
     DATE_SUB(@seed_now, INTERVAL 7 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000202'), 'GROUP', 'Team Backend',
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), NULL, NULL, NULL,
     DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000203'), 'GROUP', 'Cuối tuần đi đâu?',
     UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), NULL, NULL, NULL,
     DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    updated_at = VALUES(updated_at);

-- Active memberships.
INSERT INTO conversation_members (
    id, conversation_id, user_id, role, joined_at, left_at, created_at, updated_at
) VALUES
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000301'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'MEMBER',
     DATE_SUB(@seed_now, INTERVAL 7 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 7 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000302'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), 'MEMBER',
     DATE_SUB(@seed_now, INTERVAL 7 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 7 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000303'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'OWNER',
     DATE_SUB(@seed_now, INTERVAL 3 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000304'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000103'), 'MEMBER',
     DATE_SUB(@seed_now, INTERVAL 3 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000305'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000104'), 'MEMBER',
     DATE_SUB(@seed_now, INTERVAL 3 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 3 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000306'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000203'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000102'), 'OWNER',
     DATE_SUB(@seed_now, INTERVAL 1 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000307'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000203'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'), 'MEMBER',
     DATE_SUB(@seed_now, INTERVAL 1 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now)
ON DUPLICATE KEY UPDATE
    role = VALUES(role),
    left_at = NULL,
    updated_at = VALUES(updated_at);

-- Messages. The third conversation intentionally has no messages.
INSERT INTO messages (
    id, conversation_id, sender_id, client_message_id, content, created_at, updated_at
) VALUES
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000401'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'),
     'seed-direct-001', 'Bình ơi, test API nhé!', DATE_SUB(@seed_now, INTERVAL 10 MINUTE), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000402'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000201'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000102'),
     'seed-direct-002', 'Ok, mình đang kiểm tra đây 👌', DATE_SUB(@seed_now, INTERVAL 5 MINUTE), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000403'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000101'),
     'seed-group-001', 'Mọi người review API danh sách hội thoại nhé.',
     DATE_SUB(@seed_now, INTERVAL 8 MINUTE), @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000404'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000202'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000103'),
     'seed-group-002', 'Đã xem, response ổn rồi 🎉', DATE_SUB(@seed_now, INTERVAL 2 MINUTE), @seed_now)
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

-- Read markers create different unread counts for testing HTTP-10.
INSERT INTO conversation_read_states (
    member_id, last_read_message_id, created_at, updated_at
) VALUES
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000301'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000401'), @seed_now, @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000302'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000402'), @seed_now, @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000303'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000403'), @seed_now, @seed_now),
    (UUID_TO_BIN('00000000-0000-0000-0000-000000000304'),
     UUID_TO_BIN('00000000-0000-0000-0000-000000000404'), @seed_now, @seed_now)
ON DUPLICATE KEY UPDATE
    last_read_message_id = VALUES(last_read_message_id),
    updated_at = VALUES(updated_at);

COMMIT;

SELECT username, full_name
FROM users
WHERE username LIKE 'seed\_%' ESCAPE '\\'
ORDER BY username;
