-- name: sql-create-file<!
INSERT INTO files (filename, content_type) VALUES (:filename, :content_type);
