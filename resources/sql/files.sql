-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size) VALUES (:key, :filename, :content_type, :size);

-- name: sql-delete-file!
UPDATE files SET deleted = NOW() WHERE key = :key AND deleted IS NULL;

-- name: sql-get-file-for-update
SELECT deleted FROM files WHERE key = :key FOR UPDATE;

-- name: sql-get-metadata
SELECT key, filename, content_type, size, uploaded, deleted
  FROM files
  WHERE key IN (:keys)
  AND (deleted IS NULL OR deleted > NOW())
  AND (:av_disabled = true OR virus_scan_status = 'done');

-- name: sql-get-unscanned-file
SELECT key, filename FROM files WHERE virus_scan_status = 'not_started' LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-set-virus-scan-status!
UPDATE files SET virus_scan_status = :virus_scan_status::virus_scan_status WHERE key = :file_key;
