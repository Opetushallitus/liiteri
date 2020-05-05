UPDATE files
SET virus_scan_status = 'virus_found'::virus_scan_status
WHERE virus_scan_status = 'failed'::virus_scan_status;
