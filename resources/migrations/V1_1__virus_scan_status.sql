CREATE TYPE virus_scan_status AS ENUM('not_started', 'failed', 'done');
ALTER TABLE files ADD COLUMN virus_scan_status virus_scan_status NOT NULL DEFAULT 'not_started';
