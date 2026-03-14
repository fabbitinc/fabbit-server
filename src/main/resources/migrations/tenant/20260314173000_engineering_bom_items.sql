ALTER TABLE IF EXISTS bom_links
    RENAME TO engineering_bom_items;

ALTER TABLE engineering_bom_items
    ADD COLUMN parent_part_revision_id UUID;

ALTER TABLE engineering_bom_items
    ADD COLUMN child_part_revision_id UUID;

ALTER TABLE engineering_bom_items
    ADD COLUMN line_number VARCHAR(50);

WITH resolved_revisions AS (
    SELECT
        e.id,
        COALESCE(
            parent_part.current_released_revision_id,
            parent_part.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = parent_part.id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS parent_part_revision_id,
        COALESCE(
            child_part.current_released_revision_id,
            child_part.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = child_part.id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS child_part_revision_id
    FROM engineering_bom_items e
    JOIN parts parent_part ON parent_part.id = e.parent_part_id
    JOIN parts child_part ON child_part.id = e.child_part_id
)
UPDATE engineering_bom_items e
SET parent_part_revision_id = rr.parent_part_revision_id,
    child_part_revision_id = rr.child_part_revision_id
FROM resolved_revisions rr
WHERE e.id = rr.id;

WITH numbered_lines AS (
    SELECT
        e.id,
        (ROW_NUMBER() OVER (
            PARTITION BY e.parent_part_revision_id
            ORDER BY e.created_at ASC, e.id ASC
        ) * 10)::text AS line_number
    FROM engineering_bom_items e
)
UPDATE engineering_bom_items e
SET line_number = nl.line_number
FROM numbered_lines nl
WHERE e.id = nl.id;

ALTER TABLE engineering_bom_items
    ALTER COLUMN quantity TYPE NUMERIC(19, 6) USING quantity::numeric(19, 6);

ALTER TABLE engineering_bom_items
    ALTER COLUMN parent_part_revision_id SET NOT NULL;

ALTER TABLE engineering_bom_items
    ALTER COLUMN child_part_revision_id SET NOT NULL;

ALTER TABLE engineering_bom_items
    ALTER COLUMN line_number SET NOT NULL;

ALTER TABLE engineering_bom_items
    DROP CONSTRAINT IF EXISTS uq_bom_links_parent_part_id_child_part_id;

ALTER TABLE engineering_bom_items
    DROP CONSTRAINT IF EXISTS fk4vo0mur5mnap1tm1w4d53yfrj;

ALTER TABLE engineering_bom_items
    DROP CONSTRAINT IF EXISTS fkjves4rhgcqe65ts257oxrr145;

DROP INDEX IF EXISTS ix_bom_links_parent_part_id;
DROP INDEX IF EXISTS ix_bom_links_child_part_id;

ALTER TABLE engineering_bom_items
    DROP COLUMN parent_part_id;

ALTER TABLE engineering_bom_items
    DROP COLUMN child_part_id;

ALTER TABLE engineering_bom_items
    ADD CONSTRAINT fk_engineering_bom_items_parent_part_revision_id
        FOREIGN KEY (parent_part_revision_id) REFERENCES part_revisions (id);

ALTER TABLE engineering_bom_items
    ADD CONSTRAINT fk_engineering_bom_items_child_part_revision_id
        FOREIGN KEY (child_part_revision_id) REFERENCES part_revisions (id);

ALTER TABLE engineering_bom_items
    ADD CONSTRAINT uq_engineering_bom_items_parent_revision_line_number
        UNIQUE (parent_part_revision_id, line_number);

CREATE INDEX ix_engineering_bom_items_parent_part_revision_id
    ON engineering_bom_items (parent_part_revision_id);

CREATE INDEX ix_engineering_bom_items_child_part_revision_id
    ON engineering_bom_items (child_part_revision_id);
