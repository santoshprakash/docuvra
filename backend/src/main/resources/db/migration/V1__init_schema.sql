CREATE TABLE documents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    latest_version_number INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE document_versions (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    local_file_path TEXT NOT NULL,
    file_data BYTEA NOT NULL,
    page_count INT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_document_versions_document_version UNIQUE (document_id, version_number)
);

CREATE INDEX idx_documents_created_at ON documents (created_at);
CREATE INDEX idx_document_versions_document_id ON document_versions (document_id);
CREATE INDEX idx_document_versions_document_version ON document_versions (document_id, version_number);
CREATE INDEX idx_document_versions_uploaded_at ON document_versions (uploaded_at);

