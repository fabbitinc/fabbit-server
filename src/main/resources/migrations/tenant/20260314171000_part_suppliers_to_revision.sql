ALTER TABLE part_suppliers
    ADD COLUMN part_revision_id UUID;

WITH resolved_revisions AS (
    SELECT
        ps.id AS part_supplier_id,
        COALESCE(
            p.current_released_revision_id,
            p.current_approved_revision_id,
            (
                SELECT pr.id
                FROM part_revisions pr
                WHERE pr.part_id = ps.part_id
                ORDER BY pr.created_at DESC
                LIMIT 1
            )
        ) AS part_revision_id
    FROM part_suppliers ps
    JOIN parts p ON p.id = ps.part_id
)
UPDATE part_suppliers ps
SET part_revision_id = rr.part_revision_id
FROM resolved_revisions rr
WHERE ps.id = rr.part_supplier_id;

ALTER TABLE part_suppliers
    ALTER COLUMN part_revision_id SET NOT NULL;

ALTER TABLE part_suppliers
    ADD CONSTRAINT fk_part_suppliers_part_revision_id
        FOREIGN KEY (part_revision_id) REFERENCES part_revisions (id);

DROP INDEX IF EXISTS ix_part_suppliers_part_id;
CREATE INDEX ix_part_suppliers_part_revision_id ON part_suppliers (part_revision_id);

ALTER TABLE part_suppliers
    DROP CONSTRAINT IF EXISTS uq_part_suppliers_part_id_supplier_id;

ALTER TABLE part_suppliers
    ADD CONSTRAINT uq_part_suppliers_part_revision_id_supplier_id
        UNIQUE (part_revision_id, supplier_id);

ALTER TABLE part_suppliers
    DROP CONSTRAINT IF EXISTS fk1awqewxj3w4het4nsdwivu0ov;

ALTER TABLE part_suppliers
    DROP COLUMN part_id;
