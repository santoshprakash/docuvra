ALTER TABLE document_versions
    ADD COLUMN IF NOT EXISTS ocr_forced BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS ocr_pages (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES document_versions (id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    extracted_text TEXT NOT NULL,
    boxes_json TEXT NULL,
    extracted_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_ocr_pages_version_page UNIQUE (version_id, page_number)
);

CREATE INDEX IF NOT EXISTS idx_ocr_pages_document_version ON ocr_pages (document_id, version_id);
