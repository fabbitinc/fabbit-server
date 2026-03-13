-- liquibase formatted sql

-- changeset codex:20260314110000-1 splitStatements:false
INSERT INTO "part_previews" (
    "id",
    "created_at",
    "part_id",
    "source_type",
    "source_id",
    "conversion_status",
    "current_job_id",
    "dimension"
)
SELECT
    d."id",
    d."created_at",
    p."id",
    'DRAWING',
    d."id",
    CASE
        WHEN dsp."drawing_id" IS NOT NULL THEN 'COMPLETED'
        WHEN d."conversion_status" = 'FAILED' THEN 'FAILED'
        WHEN d."conversion_status" = 'PENDING' THEN 'PENDING'
        WHEN d."conversion_status" = 'ACTION_REQUIRED' THEN 'FAILED'
        ELSE 'PENDING'
    END,
    CASE
        WHEN d."conversion_status" = 'PENDING' THEN d."current_job_id"
        ELSE NULL
    END,
    d."dimension"
FROM "parts" p
JOIN "drawings" d ON d."id" = p."drawing_id"
LEFT JOIN "drawing_serving_projections" dsp ON dsp."drawing_id" = d."id"
WHERE NOT EXISTS (
    SELECT 1
    FROM "part_previews" pp
    WHERE pp."part_id" = p."id"
);

-- changeset codex:20260314110000-2 splitStatements:false
INSERT INTO "part_preview_artifacts" (
    "id",
    "created_at",
    "part_preview_id",
    "file_id",
    "artifact_type",
    "format",
    "storage_key",
    "content_type",
    "file_size",
    "published_at"
)
SELECT
    da."id",
    da."created_at",
    da."drawing_id",
    da."file_id",
    da."artifact_type",
    da."format",
    da."storage_key",
    da."content_type",
    da."file_size",
    da."published_at"
FROM "drawing_artifacts" da
WHERE EXISTS (
    SELECT 1
    FROM "part_previews" pp
    WHERE pp."id" = da."drawing_id"
)
AND NOT EXISTS (
    SELECT 1
    FROM "part_preview_artifacts" ppa
    WHERE ppa."id" = da."id"
);

-- changeset codex:20260314110000-3 splitStatements:false
INSERT INTO "part_preview_processing_jobs" (
    "id",
    "created_at",
    "part_preview_id",
    "pipeline_key",
    "profile_key",
    "status",
    "attempt_count",
    "started_at",
    "completed_at",
    "failure_reason"
)
SELECT
    dj."id",
    dj."created_at",
    dj."drawing_id",
    dj."pipeline_key",
    dj."profile_key",
    dj."status",
    dj."attempt_count",
    dj."started_at",
    dj."completed_at",
    dj."failure_reason"
FROM "drawing_processing_jobs" dj
WHERE EXISTS (
    SELECT 1
    FROM "part_previews" pp
    WHERE pp."id" = dj."drawing_id"
)
AND NOT EXISTS (
    SELECT 1
    FROM "part_preview_processing_jobs" ppj
    WHERE ppj."id" = dj."id"
);

-- changeset codex:20260314110000-4 splitStatements:false
INSERT INTO "part_preview_serving_projections" (
    "part_preview_id",
    "original_key",
    "pdf_key",
    "glb_key",
    "webp_key",
    "updated_at"
)
SELECT
    dsp."drawing_id",
    dsp."original_key",
    dsp."pdf_key",
    dsp."glb_key",
    dsp."webp_key",
    dsp."updated_at"
FROM "drawing_serving_projections" dsp
WHERE EXISTS (
    SELECT 1
    FROM "part_previews" pp
    WHERE pp."id" = dsp."drawing_id"
)
AND NOT EXISTS (
    SELECT 1
    FROM "part_preview_serving_projections" ppsp
    WHERE ppsp."part_preview_id" = dsp."drawing_id"
);

-- changeset codex:20260314110000-5 splitStatements:false
ALTER TABLE "drawings" DROP COLUMN IF EXISTS "conversion_status";

-- changeset codex:20260314110000-6 splitStatements:false
ALTER TABLE "drawings" DROP COLUMN IF EXISTS "current_job_id";

-- changeset codex:20260314110000-7 splitStatements:false
DROP TABLE IF EXISTS "drawing_processing_jobs";

-- changeset codex:20260314110000-8 splitStatements:false
DROP TABLE IF EXISTS "drawing_serving_projections";
