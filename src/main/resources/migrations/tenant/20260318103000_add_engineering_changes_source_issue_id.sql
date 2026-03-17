ALTER TABLE "engineering_changes"
ADD COLUMN "source_issue_id" UUID;

CREATE INDEX "ix_engineering_changes_source_issue_id"
ON "engineering_changes" USING btree("source_issue_id");
