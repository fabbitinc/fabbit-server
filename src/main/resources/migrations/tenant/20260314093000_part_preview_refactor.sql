-- liquibase formatted sql

-- changeset codex:20260314093000-1 splitStatements:false
ALTER TABLE "drawings" ADD COLUMN "part_id" UUID;

-- changeset codex:20260314093000-2 splitStatements:false
CREATE INDEX "ix_drawings_part_id" ON "drawings" USING btree ("part_id");

-- changeset codex:20260314093000-3 splitStatements:false
UPDATE "drawings" d
SET "part_id" = p."id"
FROM "parts" p
WHERE p."drawing_id" = d."id"
  AND d."part_id" IS NULL;

-- changeset codex:20260314093000-4 splitStatements:false
CREATE TABLE "part_previews" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "part_id" UUID NOT NULL,
    "source_type" VARCHAR(20),
    "source_id" UUID,
    "conversion_status" VARCHAR(30),
    "current_job_id" UUID,
    "dimension" VARCHAR(30),
    CONSTRAINT "part_previews_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_part_previews_part_id" UNIQUE ("part_id")
);

-- changeset codex:20260314093000-5 splitStatements:false
CREATE INDEX "ix_part_previews_source_type_source_id" ON "part_previews" USING btree ("source_type", "source_id");

-- changeset codex:20260314093000-6 splitStatements:false
CREATE TABLE "part_preview_artifacts" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "part_preview_id" UUID NOT NULL,
    "file_id" UUID,
    "artifact_type" VARCHAR(50) NOT NULL,
    "format" VARCHAR(30),
    "storage_key" VARCHAR(1000) NOT NULL,
    "content_type" VARCHAR(100),
    "file_size" BIGINT NOT NULL,
    "published_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "part_preview_artifacts_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_part_preview_artifacts_preview_type" UNIQUE ("part_preview_id", "artifact_type")
);

-- changeset codex:20260314093000-7 splitStatements:false
CREATE INDEX "ix_part_preview_artifacts_part_preview_id" ON "part_preview_artifacts" USING btree ("part_preview_id");

-- changeset codex:20260314093000-8 splitStatements:false
CREATE TABLE "part_preview_processing_jobs" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "part_preview_id" UUID NOT NULL,
    "pipeline_key" VARCHAR(100) NOT NULL,
    "profile_key" VARCHAR(100) NOT NULL,
    "status" VARCHAR(30) NOT NULL,
    "attempt_count" INTEGER NOT NULL,
    "started_at" TIMESTAMP WITH TIME ZONE,
    "completed_at" TIMESTAMP WITH TIME ZONE,
    "failure_reason" TEXT,
    CONSTRAINT "part_preview_processing_jobs_pkey" PRIMARY KEY ("id")
);

-- changeset codex:20260314093000-9 splitStatements:false
CREATE INDEX "ix_part_preview_processing_jobs_part_preview_id" ON "part_preview_processing_jobs" USING btree ("part_preview_id");

-- changeset codex:20260314093000-10 splitStatements:false
CREATE TABLE "part_preview_serving_projections" (
    "part_preview_id" UUID NOT NULL,
    "original_key" VARCHAR(1000),
    "pdf_key" VARCHAR(1000),
    "glb_key" VARCHAR(1000),
    "webp_key" VARCHAR(1000),
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "part_preview_serving_projections_pkey" PRIMARY KEY ("part_preview_id")
);
