-- Preserve existing read markers before removing the transitive dependency
-- last_read_message_id -> conversation_id from conversation_members.
CREATE TABLE conversation_read_states (
    member_id BINARY(16) NOT NULL PRIMARY KEY,
    last_read_message_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_read_states_member FOREIGN KEY (member_id) REFERENCES conversation_members(id),
    CONSTRAINT fk_read_states_message FOREIGN KEY (last_read_message_id) REFERENCES messages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_ci;

INSERT INTO conversation_read_states (member_id, last_read_message_id, created_at, updated_at)
SELECT id, last_read_message_id, created_at, updated_at
FROM conversation_members WHERE last_read_message_id IS NOT NULL;

ALTER TABLE conversation_members
    DROP FOREIGN KEY fk_members_last_read,
    DROP COLUMN last_read_message_id;

-- message_id already determines conversation_id; attachments store only the link.
ALTER TABLE message_attachments
    DROP FOREIGN KEY fk_attachments_message,
    DROP FOREIGN KEY fk_attachments_file,
    DROP COLUMN conversation_id,
    ADD CONSTRAINT fk_attachments_message_id FOREIGN KEY (message_id) REFERENCES messages(id),
    ADD CONSTRAINT fk_attachments_file_id FOREIGN KEY (file_id) REFERENCES files(id);

-- Keep an ordinary index needed by the file conversation FK.
CREATE INDEX idx_files_conversation ON files(conversation_id);
ALTER TABLE files DROP INDEX uk_files_conversation_id;
ALTER TABLE messages DROP INDEX uk_messages_conversation_id;

-- Cross-table business constraints cannot be CHECK expressions in MySQL.
-- Lock parent rows so concurrent changes cannot invalidate these checks.
DELIMITER //
CREATE TRIGGER trg_attachments_insert BEFORE INSERT ON message_attachments FOR EACH ROW
BEGIN
    DECLARE message_conversation BINARY(16);
    DECLARE file_conversation BINARY(16);
    SELECT conversation_id INTO message_conversation FROM messages WHERE id = NEW.message_id FOR SHARE;
    SELECT conversation_id INTO file_conversation FROM files WHERE id = NEW.file_id FOR SHARE;
    IF message_conversation IS NULL OR file_conversation IS NULL
       OR message_conversation <> file_conversation THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attachment must use a file bound to the message conversation';
    END IF;
END//
CREATE TRIGGER trg_attachments_update BEFORE UPDATE ON message_attachments FOR EACH ROW
BEGIN
    DECLARE message_conversation BINARY(16);
    DECLARE file_conversation BINARY(16);
    SELECT conversation_id INTO message_conversation FROM messages WHERE id = NEW.message_id FOR SHARE;
    SELECT conversation_id INTO file_conversation FROM files WHERE id = NEW.file_id FOR SHARE;
    IF message_conversation IS NULL OR file_conversation IS NULL
       OR message_conversation <> file_conversation THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Attachment must use a file bound to the message conversation';
    END IF;
END//
CREATE TRIGGER trg_read_states_insert BEFORE INSERT ON conversation_read_states FOR EACH ROW
BEGIN
    DECLARE member_conversation BINARY(16);
    DECLARE message_conversation BINARY(16);
    SELECT conversation_id INTO member_conversation FROM conversation_members WHERE id = NEW.member_id FOR SHARE;
    SELECT conversation_id INTO message_conversation FROM messages WHERE id = NEW.last_read_message_id FOR SHARE;
    IF member_conversation IS NULL OR message_conversation IS NULL
       OR member_conversation <> message_conversation THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Read marker must reference the member conversation';
    END IF;
END//
CREATE TRIGGER trg_read_states_update BEFORE UPDATE ON conversation_read_states FOR EACH ROW
BEGIN
    DECLARE member_conversation BINARY(16);
    DECLARE message_conversation BINARY(16);
    SELECT conversation_id INTO member_conversation FROM conversation_members WHERE id = NEW.member_id FOR SHARE;
    SELECT conversation_id INTO message_conversation FROM messages WHERE id = NEW.last_read_message_id FOR SHARE;
    IF member_conversation IS NULL OR message_conversation IS NULL
       OR member_conversation <> message_conversation THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Read marker must reference the member conversation';
    END IF;
END//
-- Parent scopes are immutable; otherwise parent updates could bypass the checks above.
CREATE TRIGGER trg_messages_scope BEFORE UPDATE ON messages FOR EACH ROW
BEGIN
    IF NOT (OLD.conversation_id <=> NEW.conversation_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Message conversation cannot change';
    END IF;
END//
CREATE TRIGGER trg_members_scope BEFORE UPDATE ON conversation_members FOR EACH ROW
BEGIN
    IF NOT (OLD.conversation_id <=> NEW.conversation_id) OR NOT (OLD.user_id <=> NEW.user_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Membership identity cannot change';
    END IF;
END//
CREATE TRIGGER trg_files_scope BEFORE UPDATE ON files FOR EACH ROW
BEGIN
    IF OLD.conversation_id IS NOT NULL AND NOT (OLD.conversation_id <=> NEW.conversation_id) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Bound file conversation cannot change';
    END IF;
END//
DELIMITER ;
