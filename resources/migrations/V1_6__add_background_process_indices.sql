create index if not exists files_virus_scan_status on files (virus_scan_status);
create index if not exists files_preview_generation_idx on files (preview_status, content_type, virus_scan_status, uploaded desc);
