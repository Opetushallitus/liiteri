-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size, version) VALUES (:key, :filename, :content_type, :size, :version);

-- name: sql-delete-file!
UPDATE files SET deleted = NOW(), delete_reason = :delete_reason WHERE key = :key AND deleted IS NULL;

-- name: sql-mark-virus-checked!
UPDATE files SET virus_checked = NOW() WHERE key = :key AND deleted IS NULL;

-- name: sql-get-non-virus-checked
SELECT key, filename, content_type, size, uploaded, version, deleted, delete_reason FROM files WHERE virus_checked IS NULL;

-- name: sql-get-file-for-update
SELECT deleted FROM files WHERE key = :key FOR UPDATE;

-- name: sql-get-metadata
SELECT key, filename, content_type, size, uploaded, deleted, delete_reason FROM files WHERE key IN (:keys) AND deleted IS NULL OR deleted > NOW();
