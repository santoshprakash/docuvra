DELETE FROM annotation_comments comment
WHERE NOT EXISTS (
    SELECT 1
    FROM annotations annotation
    WHERE annotation.id = comment.annotation_id
);

ALTER TABLE annotation_comments
    ALTER COLUMN annotation_id SET NOT NULL;

ALTER TABLE annotation_comments
    DROP CONSTRAINT IF EXISTS annotation_comments_annotation_id_fkey;

ALTER TABLE annotation_comments
    ADD CONSTRAINT annotation_comments_annotation_id_fkey
    FOREIGN KEY (annotation_id)
    REFERENCES annotations (id)
    ON DELETE CASCADE;
