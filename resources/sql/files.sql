-- name: sql-create-file<!
INSERT INTO files (filename, content_type) VALUES (:filename, :content_type);

-- name: sql-delete-file<!
UPDATE files SET deleted = NOW() WHERE id = :id AND deleted IS NULL;
