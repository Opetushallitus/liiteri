CREATE TYPE preview_generation_status AS ENUM ('not_supported', 'not_generated', 'finished', 'error');

ALTER TABLE files ADD COLUMN page_count INTEGER;
ALTER TABLE files ADD COLUMN preview_status preview_generation_status NOT NULL DEFAULT 'not_generated';

CREATE TABLE previews (
  file_id BIGSERIAL REFERENCES files(id) NOT NULL,
  page_number INTEGER NOT NULL,

  key VARCHAR(64) UNIQUE NOT NULL,
  filename VARCHAR(256) NOT NULL,
  content_type VARCHAR(128) NOT NULL DEFAULT 'image/png',
  size BIGINT NOT NULL,
  uploaded TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  deleted TIMESTAMP WITH TIME ZONE,

  final BOOLEAN NOT NULL DEFAULT FALSE,

  UNIQUE(file_id, page_number)
);

-- Only PDF previews are supported at this time. Further preview generators need to scan both
-- 'not_generated' and 'not_supported' for processing
UPDATE files SET preview_status = 'not_supported' WHERE content_type != 'application/pdf';
