-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size) VALUES (:key, :filename, :content_type, :size);

-- name: sql-delete-file!
UPDATE files SET deleted = NOW() WHERE key = :key AND deleted IS NULL;

-- name: sql-get-metadata
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, final
FROM files
WHERE key IN (:keys)
  AND (
    deleted IS NULL
    OR deleted > NOW()
    OR virus_scan_status = 'failed');

-- name: sql-get-unscanned-file
SELECT key, filename, content_type
  FROM files
  WHERE virus_scan_status = 'not_started'
  AND deleted IS NULL
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-file-without-mime-type
SELECT key, filename, content_type, uploaded
  FROM files
  WHERE content_type IS NULL
  AND deleted IS NULL
  ORDER BY uploaded DESC
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-set-virus-scan-status!
UPDATE files SET virus_scan_status = :virus_scan_status::virus_scan_status WHERE key = :file_key;

-- name: sql-finalize-files!
UPDATE files SET final = TRUE WHERE key IN (:keys);

-- name: sql-set-content-type!
UPDATE files SET content_type = :content_type WHERE key = :file_key;

-- name: sql-get-draft-file
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, final
  FROM files
  WHERE
    NOT final
    AND deleted IS NULL
    AND uploaded < NOW() - INTERVAL '1 day'
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-queue-length
SELECT count(*) AS count
FROM files
WHERE virus_scan_status = 'not_started' AND deleted IS NULL;

-- name: sql-get-oldest-unscanned-file
SELECT
  id,
  key,
  (extract(EPOCH FROM age(now(), uploaded)) :: INTEGER) AS age
FROM files
WHERE virus_scan_status = 'not_started' AND deleted IS NULL
ORDER BY age(uploaded) DESC
LIMIT 1;
