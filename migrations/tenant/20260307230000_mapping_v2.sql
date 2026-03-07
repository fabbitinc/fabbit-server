CREATE TABLE "mapping_v2_records" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "is_active" BOOLEAN NOT NULL,
    "name" VARCHAR(200) NOT NULL,
    "usage_count" INTEGER NOT NULL,
    CONSTRAINT "mapping_v2_records_pkey" PRIMARY KEY ("id")
);

CREATE TABLE "mapping_v2_revisions" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "file_id" UUID NOT NULL,
    "mapping" JSONB NOT NULL,
    "original_headers" JSONB NOT NULL,
    "record_id" UUID NOT NULL,
    "sheet_name" VARCHAR(200),
    "usage_count" INTEGER NOT NULL,
    "version" INTEGER NOT NULL,
    CONSTRAINT "mapping_v2_revisions_pkey" PRIMARY KEY ("id")
);

CREATE INDEX "ix_mapping_v2_records_is_active"
    ON "mapping_v2_records" USING btree("is_active");

ALTER TABLE "mapping_v2_records"
    ADD CONSTRAINT "uq_mapping_v2_records_name" UNIQUE ("name");

CREATE INDEX "ix_mapping_v2_revisions_record_id"
    ON "mapping_v2_revisions" USING btree("record_id");

CREATE INDEX "ix_mapping_v2_revisions_file_id"
    ON "mapping_v2_revisions" USING btree("file_id");

ALTER TABLE "mapping_v2_revisions"
    ADD CONSTRAINT "uq_mapping_v2_revisions_record_version" UNIQUE ("record_id", "version");

ALTER TABLE "mapping_v2_revisions"
    ADD CONSTRAINT "fk_mapping_v2_revisions_record_id"
    FOREIGN KEY ("record_id") REFERENCES "mapping_v2_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

ALTER TABLE "mapping_v2_revisions"
    ADD CONSTRAINT "fk_mapping_v2_revisions_file_id"
    FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;
