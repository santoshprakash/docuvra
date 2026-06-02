ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS uploaded_by_user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS uploaded_by_name VARCHAR(100) NOT NULL DEFAULT 'Staff';

UPDATE documents
SET uploaded_by_user_id = '10000000-0000-0000-0000-000000000002',
    uploaded_by_name = 'Staff User'
WHERE uploaded_by_user_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents (uploaded_by_user_id);
