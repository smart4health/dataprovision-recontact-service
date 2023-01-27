ALTER TABLE request ADD COLUMN message_content_title VARCHAR NOT NULL DEFAULT 'Title';
ALTER TABLE message ADD COLUMN content_title VARCHAR NOT NULL DEFAULT 'Title';