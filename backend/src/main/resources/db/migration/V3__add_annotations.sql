CREATE TABLE annotations (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    version_id UUID NOT NULL REFERENCES document_versions (id) ON DELETE CASCADE,
    page_number INT NOT NULL,
    annotation_type VARCHAR(40) NOT NULL,
    x_percent DOUBLE PRECISION NOT NULL,
    y_percent DOUBLE PRECISION NOT NULL,
    width_percent DOUBLE PRECISION NOT NULL,
    height_percent DOUBLE PRECISION NOT NULL,
    pixel_x DOUBLE PRECISION NOT NULL,
    pixel_y DOUBLE PRECISION NOT NULL,
    pixel_width DOUBLE PRECISION NOT NULL,
    pixel_height DOUBLE PRECISION NOT NULL,
    page_render_width DOUBLE PRECISION NOT NULL,
    page_render_height DOUBLE PRECISION NOT NULL,
    color VARCHAR(40) NULL,
    stroke_width DOUBLE PRECISION NULL,
    selected_text TEXT NULL,
    drawing_data TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE annotation_comments (
    id UUID PRIMARY KEY,
    annotation_id UUID NOT NULL REFERENCES annotations (id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_annotations_document_version ON annotations (document_id, version_id);
CREATE INDEX idx_annotations_page ON annotations (document_id, version_id, page_number);
CREATE INDEX idx_annotation_comments_annotation ON annotation_comments (annotation_id);
