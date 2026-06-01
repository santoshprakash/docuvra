ALTER TABLE document_versions
    ADD COLUMN thumbnail_data BYTEA NULL,
    ADD COLUMN thumbnail_mime_type VARCHAR(100) NULL;
