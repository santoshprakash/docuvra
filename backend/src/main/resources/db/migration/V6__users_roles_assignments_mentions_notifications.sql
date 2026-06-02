CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE document_assignments (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    assigned_to_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    assigned_by_user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE document_assignment_requests (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    status VARCHAR(40) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by_user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP NULL,
    review_comment TEXT NULL
);

CREATE TABLE comment_mentions (
    id UUID PRIMARY KEY,
    comment_id UUID NOT NULL REFERENCES annotation_comments (id) ON DELETE CASCADE,
    mentioned_user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    notification_type VARCHAR(60) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_document_id UUID NULL REFERENCES documents (id) ON DELETE CASCADE,
    related_version_id UUID NULL REFERENCES document_versions (id) ON DELETE CASCADE,
    related_annotation_id UUID NULL REFERENCES annotations (id) ON DELETE CASCADE,
    related_comment_id UUID NULL REFERENCES annotation_comments (id) ON DELETE CASCADE,
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE annotations
    ADD COLUMN created_by_user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN created_by_name VARCHAR(100) NOT NULL DEFAULT 'Staff';

ALTER TABLE annotation_comments
    ADD COLUMN created_by_user_id UUID NULL REFERENCES users (id) ON DELETE SET NULL,
    ADD COLUMN created_by_name VARCHAR(100) NOT NULL DEFAULT 'Staff';

CREATE INDEX idx_users_role ON users (role);
CREATE INDEX idx_document_assignments_document ON document_assignments (document_id);
CREATE INDEX idx_document_assignments_user ON document_assignments (assigned_to_user_id);
CREATE INDEX idx_document_assignment_requests_status ON document_assignment_requests (status);
CREATE INDEX idx_comment_mentions_comment ON comment_mentions (comment_id);
CREATE INDEX idx_comment_mentions_user ON comment_mentions (mentioned_user_id);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, read_flag);

INSERT INTO users (id, username, email, password_hash, role, active, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Normal User', 'normal@docuvra.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8TLJ8P4GDD6x1M7uWa0wM3QCDVxWq', 'NORMAL_USER', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000002', 'Staff User', 'staff@docuvra.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8TLJ8P4GDD6x1M7uWa0wM3QCDVxWq', 'STAFF', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003', 'Supervisor', 'supervisor@docuvra.local', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi8TLJ8P4GDD6x1M7uWa0wM3QCDVxWq', 'SUPERVISOR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
