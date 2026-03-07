CREATE TABLE "synthesis_v2_batches" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "accepted_count" INTEGER NOT NULL,
    "failed_uploads" JSONB NOT NULL,
    "mapping_id" UUID NOT NULL,
    "project_id" UUID,
    "requested_count" INTEGER NOT NULL,
    CONSTRAINT "synthesis_v2_batches_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "synthesis_v2_jobs" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "batch_id" UUID NOT NULL,
    "completed_at" TIMESTAMP WITH TIME ZONE,
    "errors" JSONB NOT NULL,
    "file_id" UUID NOT NULL,
    "mapping_id" UUID NOT NULL,
    "nodes_created" INTEGER NOT NULL,
    "processed_rows" INTEGER NOT NULL,
    "relationships_created" INTEGER NOT NULL,
    "started_at" TIMESTAMP WITH TIME ZONE,
    "status" VARCHAR(20) NOT NULL,
    "total_rows" INTEGER NOT NULL,
    CONSTRAINT "synthesis_v2_jobs_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "ix_synthesis_v2_batches_project_id"
    ON "synthesis_v2_batches" USING btree("project_id");

CREATE INDEX "ix_synthesis_v2_batches_mapping_id"
    ON "synthesis_v2_batches" USING btree("mapping_id");

CREATE INDEX "ix_synthesis_v2_jobs_batch_id"
    ON "synthesis_v2_jobs" USING btree("batch_id");

CREATE INDEX "ix_synthesis_v2_jobs_mapping_id"
    ON "synthesis_v2_jobs" USING btree("mapping_id");

CREATE INDEX "ix_synthesis_v2_jobs_file_id"
    ON "synthesis_v2_jobs" USING btree("file_id");

ALTER TABLE "synthesis_v2_batches"
    ADD CONSTRAINT "fk_synthesis_v2_batches_project_id"
    FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "synthesis_v2_batches"
    ADD CONSTRAINT "fk_synthesis_v2_batches_mapping_id"
    FOREIGN KEY ("mapping_id") REFERENCES "mapping_v2_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "synthesis_v2_jobs"
    ADD CONSTRAINT "fk_synthesis_v2_jobs_batch_id"
    FOREIGN KEY ("batch_id") REFERENCES "synthesis_v2_batches" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "synthesis_v2_jobs"
    ADD CONSTRAINT "fk_synthesis_v2_jobs_mapping_id"
    FOREIGN KEY ("mapping_id") REFERENCES "mapping_v2_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "synthesis_v2_jobs"
    ADD CONSTRAINT "fk_synthesis_v2_jobs_file_id"
    FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;
