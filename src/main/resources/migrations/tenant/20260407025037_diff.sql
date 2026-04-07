-- liquibase formatted sql

-- changeset moonseongha:1775497846872-1 splitStatements:false
CREATE TABLE "engineering_change_labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "engineering_change_id" UUID NOT NULL, "label_id" UUID NOT NULL, CONSTRAINT "engineering_change_labels_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775497846872-2 splitStatements:false
CREATE INDEX "ix_engineering_change_labels_ec_id" ON "engineering_change_labels" USING btree("engineering_change_id");

-- changeset moonseongha:1775497846872-3 splitStatements:false
CREATE INDEX "ix_engineering_change_labels_label_id" ON "engineering_change_labels" USING btree("label_id");

-- changeset moonseongha:1775497846872-4 splitStatements:false
ALTER TABLE "engineering_change_labels" ADD CONSTRAINT "uq_engineering_change_labels_ec_id_label_id" UNIQUE ("engineering_change_id", "label_id");

-- changeset moonseongha:1775497846872-5 splitStatements:false
ALTER TABLE "part_revision_histories" ADD "creation_source_type" VARCHAR(30);

-- changeset moonseongha:1775497846872-6 splitStatements:false
ALTER TABLE "part_revision_histories" ADD "reason" TEXT;

-- changeset moonseongha:1775497846872-7 splitStatements:false
ALTER TABLE "part_revision_histories" ADD "release_workflow_type" VARCHAR(30);

-- changeset moonseongha:1775497846872-8 splitStatements:false
ALTER TABLE "engineering_change_labels" ADD CONSTRAINT "fkdnfmmy2596g8chvmtaxnbjwpo" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775497846872-9 splitStatements:false
ALTER TABLE "part_revision_histories" DROP COLUMN "payload";

-- changeset moonseongha:1775497846872-10 splitStatements:false
ALTER TABLE "part_revision_histories" DROP COLUMN "source_type";

