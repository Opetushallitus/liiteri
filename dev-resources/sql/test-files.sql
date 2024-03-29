-- name: sql-get-metadata-for-tests
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, virus_scan_retry_count, final, origin_system, origin_reference
  FROM files
  WHERE key IN (:keys);

-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size, uploaded, origin_system, origin_reference) VALUES (:key, :filename, :content_type, :size, :uploaded, :origin_system, :origin_reference);

-- name: sql-finalize-files!
UPDATE files SET final = TRUE, origin_system = :origin_system, origin_reference = :origin_reference WHERE key IN (:keys);