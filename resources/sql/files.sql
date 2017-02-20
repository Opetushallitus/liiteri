-- name: sql-create-file<!
INSERT INTO files (id, filename, content_type) VALUES (:id, :filename, :content_type);

-- name: sql-delete-file!
UPDATE file_versions SET deleted = NOW() WHERE file_id = :file_id AND deleted IS NULL;

-- name: sql-create-version<!
INSERT INTO file_versions (file_id, version) VALUES (:file_id, :version);
