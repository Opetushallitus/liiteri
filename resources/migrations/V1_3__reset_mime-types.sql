ALTER TABLE files ALTER COLUMN content_type DROP NOT NULL;
UPDATE files set content_type = null;
