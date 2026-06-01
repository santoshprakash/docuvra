CREATE TABLE excel_comments (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES document_versions (id) ON DELETE CASCADE,
    sheet_index INT NOT NULL,
    sheet_name VARCHAR(255) NOT NULL,
    start_cell VARCHAR(20) NOT NULL,
    end_cell VARCHAR(20) NULL,
    start_row INT NOT NULL,
    start_column INT NOT NULL,
    end_row INT NULL,
    end_column INT NULL,
    comment_text TEXT NOT NULL,
    created_by_name VARCHAR(100) NOT NULL DEFAULT 'Staff',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_excel_comments_version_sheet ON excel_comments (version_id, sheet_index);
CREATE INDEX idx_excel_comments_cell ON excel_comments (version_id, sheet_index, start_row, start_column);
CREATE INDEX idx_excel_comments_document_version ON excel_comments (document_id, version_id);
