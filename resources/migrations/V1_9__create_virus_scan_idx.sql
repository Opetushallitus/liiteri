create index if not exists files_virus_scan_idx on files (uploaded DESC) where deleted is null and (virus_scan_status = 'not_started' or virus_scan_status = 'needs_retry');
