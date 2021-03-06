-- name: sql-get-metadata-for-tests
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, virus_scan_retry_count, final
  FROM files
  WHERE key IN (:keys);

-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size, uploaded) VALUES (:key, :filename, :content_type, :size, :uploaded);

-- name: sql-finalize-files!
UPDATE files SET final = TRUE WHERE key IN (:keys);