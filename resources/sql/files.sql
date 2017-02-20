-- name: sql-create-file<!
INSERT INTO files (filename, content_type) VALUES (:filename, :content_type);

-- name: sql-delete-file!
UPDATE file_versions SET deleted = NOW() WHERE file_id = :file_id AND deleted IS NULL;

-- name: sql-create-version<!
INSERT INTO file_versions (file_id, version) VALUES (:file_id, :version);
