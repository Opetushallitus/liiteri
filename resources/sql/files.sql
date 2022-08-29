-- name: sql-create-file<!
INSERT INTO files (key, filename, content_type, size, origin_system, origin_reference) VALUES (:key, :filename, :content_type, :size, :origin_system, :origin_reference);

-- name: sql-set-file-page-count-and-preview-status!
update files SET page_count = :page_count, preview_status = :preview_status::preview_generation_status WHERE key = :key;

-- name: sql-delete-file!
UPDATE files SET deleted = NOW(), deleted_by = :deleted_by WHERE key = :key AND deleted IS NULL;

-- name: sql-delete-file-permanently!
DELETE FROM files WHERE key = :key;

-- name: sql-delete-preview-permanently!
DELETE FROM previews WHERE file_id IN (SELECT id FROM files WHERE key = :key);

-- name: sql-delete-preview!
UPDATE previews SET deleted = NOW() WHERE key = :key AND deleted IS NULL;

-- name: sql-get-metadata
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, final, preview_status, page_count
FROM files
WHERE key IN (:keys)
  AND (
    deleted IS NULL
    OR deleted > NOW()
    OR virus_scan_status = 'failed'
    OR virus_scan_status = 'virus_found')
UNION
SELECT key, filename, content_type, size, uploaded, deleted, 'done'::virus_scan_status, true, 'not_supported'::preview_generation_status, 1
FROM previews
WHERE key IN (:keys)
  AND (
    deleted IS NULL
    OR deleted > NOW());

-- name: sql-create-preview<!
INSERT INTO previews (file_id, page_number, key, filename, content_type, size)
SELECT id, :page_number, :key, :filename, :content_type, :size
FROM files
WHERE key = :file_key;

-- name: sql-get-previews
SELECT p.key,  p.content_type, p.size, p.uploaded, p.deleted
FROM previews p
JOIN files f on p.file_id = f.id
WHERE f.key = :file_key
ORDER BY page_number ASC;

-- name: sql-get-unscanned-file
SELECT key, filename, content_type
  FROM files
  WHERE (
      virus_scan_status = 'not_started'
      OR (virus_scan_status = 'needs_retry' AND
          virus_scan_retry_after < now())
  )
  AND deleted IS NULL
  AND final = TRUE
  ORDER BY uploaded ASC
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-file-without-mime-type
SELECT key, filename, content_type, uploaded
  FROM files
  WHERE content_type IS NULL
  AND deleted IS NULL
  ORDER BY uploaded DESC
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-file-without-preview
SELECT key, filename, content_type, uploaded
FROM files
WHERE content_type IN (:content_types)
AND deleted IS NULL
AND preview_status = 'not_generated'
AND virus_scan_status = 'done'
ORDER BY uploaded DESC
LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-mark-previews-final!
UPDATE previews SET final = true where file_id = (select id from files where key = :file_key);

-- name: sql-set-virus-scan-status!
UPDATE files SET virus_scan_status = :virus_scan_status::virus_scan_status WHERE key = :file_key;

-- name: sql-mark-virus-scan-for-retry-or-fail
UPDATE files
SET virus_scan_retry_count = virus_scan_retry_count + 1,
    virus_scan_status = CASE WHEN (virus_scan_retry_count < :retry_max_count - 1) THEN 'needs_retry'::virus_scan_status ELSE 'failed'::virus_scan_status END,
    virus_scan_retry_after = now () + (virus_scan_retry_count + 1) * :retry_wait_minutes * interval '1 minutes'
WHERE key = :file_key
RETURNING virus_scan_status, virus_scan_retry_count;

-- name: sql-finalize-files!
UPDATE files SET final = TRUE, origin_system = :origin_system, origin_reference = :origin_reference WHERE key IN (:keys);

-- name: sql-set-content-type-and-filename!
UPDATE files SET content_type = :content_type, filename = :filename WHERE key = :file_key;

-- name: sql-get-draft-file
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, final
  FROM files
  WHERE
    NOT final
    AND deleted IS NULL
    AND uploaded < NOW() - INTERVAL '1 day'
  LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-old-deleted-file
SELECT key, filename, content_type, size, uploaded, deleted, virus_scan_status, final
FROM files
WHERE
    NOT final
  AND deleted IS NOT NULL
  AND deleted < NOW() - INTERVAL '180 day'
    LIMIT 1 FOR UPDATE SKIP LOCKED;

-- name: sql-get-draft-preview
SELECT key, filename, content_type, size, uploaded, deleted, final
FROM previews
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

-- name: sql-update-filename!
UPDATE files SET filename = :filename WHERE key = :file_key;

-- name: sql-get-file-keys-by-origin-references
SELECT key
FROM files
WHERE origin_reference IN (:origin_references) AND deleted IS NULL;

