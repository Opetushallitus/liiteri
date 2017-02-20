ALTER TABLE file_versions DROP CONSTRAINT file_versions_file_id_fkey;
DROP SEQUENCE files_id_seq CASCADE;
ALTER TABLE files ALTER COLUMN id TYPE VARCHAR(255);
ALTER TABLE file_versions ALTER COLUMN file_id TYPE VARCHAR(255);
ALTER TABLE file_versions ADD CONSTRAINT file_versions_file_id_fkey FOREIGN KEY (file_id) REFERENCES files(id);
