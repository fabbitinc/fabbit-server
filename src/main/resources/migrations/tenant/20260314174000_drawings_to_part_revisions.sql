ALTER TABLE drawings
    ADD COLUMN part_revision_id UUID;

WITH resolved_revisions AS (
    SELECT
        d.id AS drawing_id,
        COALESCE(
            p.current_released_revision_id,
            p.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = d.part_id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS part_revision_id
    FROM drawings d
    JOIN parts p ON p.id = d.part_id
    WHERE d.part_id IS NOT NULL
)
UPDATE drawings d
SET part_revision_id = rr.part_revision_id
FROM resolved_revisions rr
WHERE d.id = rr.drawing_id;

ALTER TABLE drawings
    ADD CONSTRAINT fk_drawings_part_revision_id
        FOREIGN KEY (part_revision_id) REFERENCES part_revisions (id);

DROP INDEX IF EXISTS ix_drawings_part_id;
CREATE INDEX ix_drawings_part_revision_id ON drawings (part_revision_id);

ALTER TABLE drawings
    DROP COLUMN part_id;
