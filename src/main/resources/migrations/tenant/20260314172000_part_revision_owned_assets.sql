ALTER TABLE part_revisions
    ADD COLUMN owner_id UUID;

ALTER TABLE part_revisions
    ADD COLUMN owner_team_id UUID;

UPDATE part_revisions pr
SET owner_id = p.owner_id,
    owner_team_id = p.owner_team_id
FROM parts p
WHERE pr.part_id = p.id;

ALTER TABLE part_revisions
    ADD CONSTRAINT fk_part_revisions_owner_id
        FOREIGN KEY (owner_id) REFERENCES users (id);

ALTER TABLE part_revisions
    ADD CONSTRAINT fk_part_revisions_owner_team_id
        FOREIGN KEY (owner_team_id) REFERENCES teams (id);

CREATE INDEX ix_part_revisions_owner_id ON part_revisions (owner_id);
CREATE INDEX ix_part_revisions_owner_team_id ON part_revisions (owner_team_id);

ALTER TABLE part_previews
    ADD COLUMN part_revision_id UUID;

WITH resolved_revisions AS (
    SELECT
        pp.id AS part_preview_id,
        COALESCE(
            p.current_released_revision_id,
            p.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = pp.part_id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS part_revision_id
    FROM part_previews pp
    JOIN parts p ON p.id = pp.part_id
)
UPDATE part_previews pp
SET part_revision_id = rr.part_revision_id
FROM resolved_revisions rr
WHERE pp.id = rr.part_preview_id;

ALTER TABLE part_previews
    ALTER COLUMN part_revision_id SET NOT NULL;

ALTER TABLE part_previews
    ADD CONSTRAINT fk_part_previews_part_revision_id
        FOREIGN KEY (part_revision_id) REFERENCES part_revisions (id);

ALTER TABLE part_previews
    DROP CONSTRAINT IF EXISTS uq_part_previews_part_id;

ALTER TABLE part_previews
    ADD CONSTRAINT uq_part_previews_part_revision_id
        UNIQUE (part_revision_id);

ALTER TABLE part_previews
    DROP COLUMN part_id;

WITH resolved_revisions AS (
    SELECT
        f.id AS file_id,
        COALESCE(
            p.current_released_revision_id,
            p.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = p.id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS part_revision_id
    FROM files f
    JOIN parts p ON p.id = f.owner_id
    WHERE f.owner_type = 'part'
)
UPDATE files f
SET owner_type = 'part_revision',
    owner_id = rr.part_revision_id
FROM resolved_revisions rr
WHERE f.id = rr.file_id;
