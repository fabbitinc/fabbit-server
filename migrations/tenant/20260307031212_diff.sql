-- liquibase formatted sql

-- changeset seongha.moon:1772820736312-1 splitStatements:false
CREATE TABLE "activities" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action" VARCHAR(50) NOT NULL, "actor_id" UUID NOT NULL, "detail" JSONB, "target_id" UUID NOT NULL, "target_type" VARCHAR(20) NOT NULL, CONSTRAINT "activities_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-2 splitStatements:false
CREATE TABLE "bom_links" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "child_part_id" UUID NOT NULL, "extended_properties" JSONB NOT NULL, "parent_part_id" UUID NOT NULL, "quantity" INTEGER NOT NULL, CONSTRAINT "bom_links_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-3 splitStatements:false
CREATE TABLE "change_request_issues" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "change_request_id" UUID NOT NULL, "issue_id" UUID NOT NULL, CONSTRAINT "change_request_issues_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-4 splitStatements:false
CREATE TABLE "change_request_reviewers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "change_request_id" UUID NOT NULL, "review_status" VARCHAR(20) NOT NULL, "reviewed_at" TIMESTAMP WITH TIME ZONE, "user_id" UUID NOT NULL, CONSTRAINT "change_request_reviewers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-5 splitStatements:false
CREATE TABLE "cr_team_reviewers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "change_request_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "cr_team_reviewers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-6 splitStatements:false
CREATE TABLE "files" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "content_type" VARCHAR(100) NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "file_key" VARCHAR(1000) NOT NULL, "file_size" BIGINT NOT NULL, "original_name" VARCHAR(500) NOT NULL, "owner_id" UUID, "owner_type" VARCHAR(50), "status" VARCHAR(20) NOT NULL, CONSTRAINT "files_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-7 splitStatements:false
CREATE TABLE "issue_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "issue_assignees_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-8 splitStatements:false
CREATE TABLE "issue_comments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "body" TEXT NOT NULL, "created_by" UUID NOT NULL, "issue_id" UUID NOT NULL, "updated_by" UUID NOT NULL, CONSTRAINT "issue_comments_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-9 splitStatements:false
CREATE TABLE "issue_labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "label_id" UUID NOT NULL, CONSTRAINT "issue_labels_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-10 splitStatements:false
CREATE TABLE "issue_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "part_id" UUID NOT NULL, CONSTRAINT "issue_parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-11 splitStatements:false
CREATE TABLE "issue_team_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "issue_team_assignees_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-12 splitStatements:false
CREATE TABLE "issues" ("type" VARCHAR(20) NOT NULL, "id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "body" TEXT, "closed_at" TIMESTAMP WITH TIME ZONE, "created_by" UUID NOT NULL, "number" INTEGER NOT NULL, "state" VARCHAR(20) NOT NULL, "title" VARCHAR(500) NOT NULL, "updated_by" UUID NOT NULL, CONSTRAINT "issues_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-13 splitStatements:false
CREATE TABLE "mapping_records" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "name" VARCHAR(200) NOT NULL, "scope" VARCHAR(20) NOT NULL, "usage_count" INTEGER NOT NULL, CONSTRAINT "mapping_records_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-14 splitStatements:false
CREATE TABLE "mapping_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "file_id" UUID NOT NULL, "mapping" JSONB NOT NULL, "original_headers" JSONB NOT NULL, "record_id" UUID NOT NULL, "sheet_name" VARCHAR(200), "usage_count" INTEGER NOT NULL, "version" INTEGER NOT NULL, CONSTRAINT "mapping_revisions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-15 splitStatements:false
CREATE TABLE "notifications" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "actor_id" UUID NOT NULL, "payload" JSONB NOT NULL, "read_at" TIMESTAMP WITH TIME ZONE, "type" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "notifications_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-16 splitStatements:false
CREATE TABLE "part_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "category" VARCHAR(100), "description" TEXT, "drawing_id" UUID, "extended_properties" JSONB NOT NULL, "lead_time_days" INTEGER, "lifecycle_state" VARCHAR(50), "material" VARCHAR(200), "name" VARCHAR(500), "part_id" UUID NOT NULL, "part_number" VARCHAR(100) NOT NULL, "is_phantom" BOOLEAN, "revision" VARCHAR(50) NOT NULL, "synthesis_job_id" UUID, "unit" VARCHAR(20), CONSTRAINT "part_revisions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-17 splitStatements:false
CREATE TABLE "part_suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "extended_properties" JSONB NOT NULL, "part_id" UUID NOT NULL, "supplier_id" UUID NOT NULL, "unit_cost" FLOAT8, CONSTRAINT "part_suppliers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-18 splitStatements:false
CREATE TABLE "project_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "project_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "project_members_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-19 splitStatements:false
CREATE TABLE "project_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "part_id" UUID NOT NULL, "project_id" UUID NOT NULL, CONSTRAINT "project_parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-20 splitStatements:false
CREATE TABLE "suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "code" VARCHAR(100), "company_name" VARCHAR(200) NOT NULL, "contact_info" TEXT, "country" VARCHAR(100), "extended_properties" JSONB NOT NULL, CONSTRAINT "suppliers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-21 splitStatements:false
CREATE TABLE "synthesis_batches" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "accepted_count" INTEGER NOT NULL, "failed_uploads" JSONB NOT NULL, "mapping_id" UUID NOT NULL, "project_id" UUID, "requested_count" INTEGER NOT NULL, CONSTRAINT "synthesis_batches_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-22 splitStatements:false
CREATE TABLE "synthesis_jobs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "batch_id" UUID NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "errors" JSONB NOT NULL, "file_id" UUID NOT NULL, "mapping_id" UUID NOT NULL, "nodes_created" INTEGER NOT NULL, "processed_rows" INTEGER NOT NULL, "relationships_created" INTEGER NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(20) NOT NULL, "total_rows" INTEGER NOT NULL, CONSTRAINT "synthesis_jobs_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-23 splitStatements:false
CREATE TABLE "team_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "user_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "team_members_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-24 splitStatements:false
CREATE TABLE "change_requests" ("cr_state" VARCHAR(20) NOT NULL, "merged_at" TIMESTAMP WITH TIME ZONE, "merged_by" UUID, "id" UUID NOT NULL, CONSTRAINT "change_requests_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-25 splitStatements:false
CREATE TABLE "drawings" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "conversion_status" VARCHAR(30), "deleted_at" TIMESTAMP WITH TIME ZONE, "drawing_number" VARCHAR(100), "name" VARCHAR(500), "original_file_key" VARCHAR(1000), "pdf_key" VARCHAR(1000), "status" VARCHAR(50), "thumbnail_key" VARCHAR(1000), "version" VARCHAR(50), CONSTRAINT "drawings_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-26 splitStatements:false
CREATE TABLE "parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "category" VARCHAR(100), "description" TEXT, "drawing_id" UUID, "extended_properties" JSONB NOT NULL, "lead_time_days" INTEGER, "lifecycle_state" VARCHAR(50), "material" VARCHAR(200), "name" VARCHAR(500), "owner_id" UUID, "owner_team_id" UUID, "part_number" VARCHAR(100) NOT NULL, "is_phantom" BOOLEAN, "revision" VARCHAR(50) NOT NULL, "unit" VARCHAR(20), CONSTRAINT "parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-27 splitStatements:false
CREATE TABLE "labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "color" VARCHAR(7) NOT NULL, "created_by" UUID, "description" VARCHAR(200), "name" VARCHAR(50) NOT NULL, "updated_by" UUID, CONSTRAINT "labels_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-28 splitStatements:false
CREATE TABLE "part_default_owners" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "category" VARCHAR(100), "default_owner_id" UUID, "default_owner_team_id" UUID, CONSTRAINT "part_default_owners_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-29 splitStatements:false
CREATE TABLE "projects" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" VARCHAR(100), "updated_by" VARCHAR(100), "is_deleted" BOOLEAN NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "deleted_by" VARCHAR(100), "is_archived" BOOLEAN NOT NULL, "description" TEXT, "name" VARCHAR(200) NOT NULL, CONSTRAINT "projects_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-30 splitStatements:false
CREATE TABLE "teams" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID NOT NULL, "description" TEXT, "name" VARCHAR(100) NOT NULL, CONSTRAINT "teams_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1772820736312-31 splitStatements:false
CREATE INDEX "ix_activities_target" ON "activities" USING btree("target_type", "target_id");

-- changeset seongha.moon:1772820736312-32 splitStatements:false
CREATE INDEX "ix_bom_links_parent_part_id" ON "bom_links" USING btree("parent_part_id");

-- changeset seongha.moon:1772820736312-33 splitStatements:false
CREATE INDEX "ix_bom_links_child_part_id" ON "bom_links" USING btree("child_part_id");

-- changeset seongha.moon:1772820736312-34 splitStatements:false
ALTER TABLE "bom_links" ADD CONSTRAINT "uq_bom_links_parent_part_id_child_part_id" UNIQUE ("parent_part_id", "child_part_id");

-- changeset seongha.moon:1772820736312-35 splitStatements:false
CREATE INDEX "ix_change_request_issues_change_request_id" ON "change_request_issues" USING btree("change_request_id");

-- changeset seongha.moon:1772820736312-36 splitStatements:false
CREATE INDEX "ix_change_request_issues_issue_id" ON "change_request_issues" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-37 splitStatements:false
ALTER TABLE "change_request_issues" ADD CONSTRAINT "uq_change_request_issues_cr_id_issue_id" UNIQUE ("change_request_id", "issue_id");

-- changeset seongha.moon:1772820736312-38 splitStatements:false
CREATE INDEX "ix_cr_reviewers_change_request_id" ON "change_request_reviewers" USING btree("change_request_id");

-- changeset seongha.moon:1772820736312-39 splitStatements:false
CREATE INDEX "ix_cr_reviewers_user_id" ON "change_request_reviewers" USING btree("user_id");

-- changeset seongha.moon:1772820736312-40 splitStatements:false
ALTER TABLE "change_request_reviewers" ADD CONSTRAINT "uq_cr_reviewers_cr_id_user_id" UNIQUE ("change_request_id", "user_id");

-- changeset seongha.moon:1772820736312-41 splitStatements:false
CREATE INDEX "ix_cr_team_reviewers_change_request_id" ON "cr_team_reviewers" USING btree("change_request_id");

-- changeset seongha.moon:1772820736312-42 splitStatements:false
CREATE INDEX "ix_cr_team_reviewers_team_id" ON "cr_team_reviewers" USING btree("team_id");

-- changeset seongha.moon:1772820736312-43 splitStatements:false
ALTER TABLE "cr_team_reviewers" ADD CONSTRAINT "uq_cr_team_reviewers_cr_id_team_id" UNIQUE ("change_request_id", "team_id");

-- changeset seongha.moon:1772820736312-44 splitStatements:false
CREATE INDEX "ix_files_owner_type_owner_id" ON "files" USING btree("owner_type", "owner_id");

-- changeset seongha.moon:1772820736312-45 splitStatements:false
ALTER TABLE "files" ADD CONSTRAINT "uq_files_file_key" UNIQUE ("file_key");

-- changeset seongha.moon:1772820736312-46 splitStatements:false
CREATE INDEX "ix_issue_assignees_issue_id" ON "issue_assignees" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-47 splitStatements:false
CREATE INDEX "ix_issue_assignees_user_id" ON "issue_assignees" USING btree("user_id");

-- changeset seongha.moon:1772820736312-48 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "uq_issue_assignees_issue_id_user_id" UNIQUE ("issue_id", "user_id");

-- changeset seongha.moon:1772820736312-49 splitStatements:false
CREATE INDEX "ix_issue_comments_issue_id" ON "issue_comments" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-50 splitStatements:false
CREATE INDEX "ix_issue_labels_issue_id" ON "issue_labels" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-51 splitStatements:false
CREATE INDEX "ix_issue_labels_label_id" ON "issue_labels" USING btree("label_id");

-- changeset seongha.moon:1772820736312-52 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "uq_issue_labels_issue_id_label_id" UNIQUE ("issue_id", "label_id");

-- changeset seongha.moon:1772820736312-53 splitStatements:false
CREATE INDEX "ix_issue_parts_issue_id" ON "issue_parts" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-54 splitStatements:false
CREATE INDEX "ix_issue_parts_part_id" ON "issue_parts" USING btree("part_id");

-- changeset seongha.moon:1772820736312-55 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "uq_issue_parts_issue_id_part_id" UNIQUE ("issue_id", "part_id");

-- changeset seongha.moon:1772820736312-56 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_issue_id" ON "issue_team_assignees" USING btree("issue_id");

-- changeset seongha.moon:1772820736312-57 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_team_id" ON "issue_team_assignees" USING btree("team_id");

-- changeset seongha.moon:1772820736312-58 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "uq_issue_team_assignees_issue_id_team_id" UNIQUE ("issue_id", "team_id");

-- changeset seongha.moon:1772820736312-59 splitStatements:false
CREATE INDEX "ix_issues_type_state" ON "issues" USING btree("type", "state");

-- changeset seongha.moon:1772820736312-60 splitStatements:false
CREATE INDEX "ix_issues_created_by" ON "issues" USING btree("created_by");

-- changeset seongha.moon:1772820736312-61 splitStatements:false
ALTER TABLE "issues" ADD CONSTRAINT "uq_issues_number" UNIQUE ("number");

-- changeset seongha.moon:1772820736312-62 splitStatements:false
CREATE INDEX "ix_mapping_records_scope_is_active" ON "mapping_records" USING btree("scope", "is_active");

-- changeset seongha.moon:1772820736312-63 splitStatements:false
ALTER TABLE "mapping_records" ADD CONSTRAINT "uq_mapping_records_name" UNIQUE ("name");

-- changeset seongha.moon:1772820736312-64 splitStatements:false
CREATE INDEX "ix_mapping_revisions_record_id" ON "mapping_revisions" USING btree("record_id");

-- changeset seongha.moon:1772820736312-65 splitStatements:false
CREATE INDEX "ix_mapping_revisions_file_id" ON "mapping_revisions" USING btree("file_id");

-- changeset seongha.moon:1772820736312-66 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "uq_mapping_revisions_record_version" UNIQUE ("record_id", "version");

-- changeset seongha.moon:1772820736312-67 splitStatements:false
CREATE INDEX "ix_notifications_user_unread" ON "notifications" USING btree("user_id", "read_at");

-- changeset seongha.moon:1772820736312-68 splitStatements:false
CREATE INDEX "ix_part_revisions_part_id" ON "part_revisions" USING btree("part_id");

-- changeset seongha.moon:1772820736312-69 splitStatements:false
CREATE INDEX "ix_part_revisions_synthesis_job_id" ON "part_revisions" USING btree("synthesis_job_id");

-- changeset seongha.moon:1772820736312-70 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "uq_part_revisions_part_id_revision" UNIQUE ("part_id", "revision");

-- changeset seongha.moon:1772820736312-71 splitStatements:false
CREATE INDEX "ix_part_suppliers_part_id" ON "part_suppliers" USING btree("part_id");

-- changeset seongha.moon:1772820736312-72 splitStatements:false
CREATE INDEX "ix_part_suppliers_supplier_id" ON "part_suppliers" USING btree("supplier_id");

-- changeset seongha.moon:1772820736312-73 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "uq_part_suppliers_part_id_supplier_id" UNIQUE ("part_id", "supplier_id");

-- changeset seongha.moon:1772820736312-74 splitStatements:false
CREATE INDEX "ix_project_members_project_id" ON "project_members" USING btree("project_id");

-- changeset seongha.moon:1772820736312-75 splitStatements:false
CREATE INDEX "ix_project_members_user_id" ON "project_members" USING btree("user_id");

-- changeset seongha.moon:1772820736312-76 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "uq_project_members_project_id_user_id" UNIQUE ("project_id", "user_id");

-- changeset seongha.moon:1772820736312-77 splitStatements:false
CREATE INDEX "ix_project_parts_project_id" ON "project_parts" USING btree("project_id");

-- changeset seongha.moon:1772820736312-78 splitStatements:false
CREATE INDEX "ix_project_parts_part_id" ON "project_parts" USING btree("part_id");

-- changeset seongha.moon:1772820736312-79 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "uq_project_parts_project_id_part_id" UNIQUE ("project_id", "part_id");

-- changeset seongha.moon:1772820736312-80 splitStatements:false
CREATE INDEX "ix_suppliers_code" ON "suppliers" USING btree("code");

-- changeset seongha.moon:1772820736312-81 splitStatements:false
ALTER TABLE "suppliers" ADD CONSTRAINT "uq_suppliers_company_name" UNIQUE ("company_name");

-- changeset seongha.moon:1772820736312-82 splitStatements:false
CREATE INDEX "ix_synthesis_batches_project_id" ON "synthesis_batches" USING btree("project_id");

-- changeset seongha.moon:1772820736312-83 splitStatements:false
CREATE INDEX "ix_synthesis_batches_mapping_id" ON "synthesis_batches" USING btree("mapping_id");

-- changeset seongha.moon:1772820736312-84 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_batch_id" ON "synthesis_jobs" USING btree("batch_id");

-- changeset seongha.moon:1772820736312-85 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_mapping_id" ON "synthesis_jobs" USING btree("mapping_id");

-- changeset seongha.moon:1772820736312-86 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_file_id" ON "synthesis_jobs" USING btree("file_id");

-- changeset seongha.moon:1772820736312-87 splitStatements:false
CREATE INDEX "ix_team_members_team_id" ON "team_members" USING btree("team_id");

-- changeset seongha.moon:1772820736312-88 splitStatements:false
CREATE INDEX "ix_team_members_user_id" ON "team_members" USING btree("user_id");

-- changeset seongha.moon:1772820736312-89 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "uq_team_members_team_id_user_id" UNIQUE ("team_id", "user_id");

-- changeset seongha.moon:1772820736312-90 splitStatements:false
ALTER TABLE "drawings" ADD CONSTRAINT "uq_drawings_drawing_number" UNIQUE ("drawing_number");

-- changeset seongha.moon:1772820736312-91 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "uq_parts_part_number" UNIQUE ("part_number");

-- changeset seongha.moon:1772820736312-92 splitStatements:false
ALTER TABLE "labels" ADD CONSTRAINT "uq_labels_name" UNIQUE ("name");

-- changeset seongha.moon:1772820736312-93 splitStatements:false
ALTER TABLE "teams" ADD CONSTRAINT "uq_teams_name" UNIQUE ("name");

-- changeset seongha.moon:1772820736312-94 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fk1awqewxj3w4het4nsdwivu0ov" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-95 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk2w9t9pm4a18rihij6xcup7kun" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-96 splitStatements:false
ALTER TABLE "bom_links" ADD CONSTRAINT "fk4vo0mur5mnap1tm1w4d53yfrj" FOREIGN KEY ("parent_part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-97 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "fk56bv9knlohikvliauqx0s6x35" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-98 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk5ddsht7he9hab9xu47wnook3f" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-99 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fk5hquogokt71fc6i4j2tybt8rh" FOREIGN KEY ("drawing_id") REFERENCES "drawings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-100 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk6gym46gcwslhqmyr9e2ih59lr" FOREIGN KEY ("record_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-101 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "fk81q8jhewj6k8k5s1y4cw1wdyo" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-102 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fk9m48269i6u7mb5l35vixtmbal" FOREIGN KEY ("synthesis_job_id") REFERENCES "synthesis_jobs" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-103 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk_mapping_revisions_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-104 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-105 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_project_id" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-106 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-107 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-108 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "fk_team_members_team_id" FOREIGN KEY ("team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-109 splitStatements:false
ALTER TABLE "change_request_reviewers" ADD CONSTRAINT "fkakqs6cndklg6a54kog32ui0oa" FOREIGN KEY ("change_request_id") REFERENCES "change_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-110 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fkd47ukwxcpdb87h3b6kd017k9n" FOREIGN KEY ("supplier_id") REFERENCES "suppliers" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-111 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "fkdki1sp2homqsdcvqm9yrix31g" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-112 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "fkeee47ncsdhj0917jfcjnrbrvv" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-113 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fkf4vylyr6m7ofb2jd28fla6ry7" FOREIGN KEY ("batch_id") REFERENCES "synthesis_batches" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-114 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "fkfskpaxuixega1rlf9hrhnhngs" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-115 splitStatements:false
ALTER TABLE "part_default_owners" ADD CONSTRAINT "fkhl5q7rnao75wlh8lvv20uq0in" FOREIGN KEY ("default_owner_team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-116 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "fki8is5lhkwmnm2cuca1t9joveb" FOREIGN KEY ("drawing_id") REFERENCES "drawings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-117 splitStatements:false
ALTER TABLE "cr_team_reviewers" ADD CONSTRAINT "fkji8xm2rcnd3owfx372kgv8mnt" FOREIGN KEY ("change_request_id") REFERENCES "change_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-118 splitStatements:false
ALTER TABLE "bom_links" ADD CONSTRAINT "fkjves4rhgcqe65ts257oxrr145" FOREIGN KEY ("child_part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-119 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fkl1cfng9egr5y8m0chwm32y9di" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-120 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "fkl3syou2tg9nvbt8xij66s8tbw" FOREIGN KEY ("owner_team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-121 splitStatements:false
ALTER TABLE "change_requests" ADD CONSTRAINT "fklbxmcabro1hxy8yx414vtb86y" FOREIGN KEY ("id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-122 splitStatements:false
ALTER TABLE "issue_comments" ADD CONSTRAINT "fknvnj0204928o0w1th5jsx4f28" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1772820736312-123 splitStatements:false
ALTER TABLE "change_request_issues" ADD CONSTRAINT "fkok4p5qo14wu0yw2ullbbsiohk" FOREIGN KEY ("change_request_id") REFERENCES "change_requests" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

