-- liquibase formatted sql

-- changeset seongha.moon:1774060140187-1 splitStatements:false
CREATE TABLE "ai_usage_events" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "billable_amount" numeric(12, 2) NOT NULL, "billing_status" VARCHAR(20) NOT NULL, "category" VARCHAR(30) NOT NULL, "credits_used" numeric(12, 4) NOT NULL, "feature" VARCHAR(50) NOT NULL, "input_tokens" INTEGER NOT NULL, "metadata" JSONB NOT NULL, "model" VARCHAR(50) NOT NULL, "org_id" UUID NOT NULL, "output_tokens" INTEGER NOT NULL, "plan_type_snapshot" VARCHAR(20) NOT NULL, "seat_type_snapshot" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "ai_usage_events_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-2 splitStatements:false
CREATE TABLE "drawings" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "dimension" VARCHAR(30), "drawing_number" VARCHAR(100), "name" VARCHAR(500), "original_file_key" VARCHAR(1000), "part_revision_id" UUID, "source_file_id" UUID, "source_type" VARCHAR(30), "status" VARCHAR(50), "version" VARCHAR(50), CONSTRAINT "drawings_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-3 splitStatements:false
CREATE TABLE "engineering_bom_items" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "child_part_revision_id" UUID NOT NULL, "extended_properties" JSONB NOT NULL, "line_number" VARCHAR(50) NOT NULL, "parent_part_revision_id" UUID NOT NULL, "quantity" numeric(19, 6) NOT NULL, CONSTRAINT "engineering_bom_items_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-4 splitStatements:false
CREATE TABLE "engineering_change_comments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT NOT NULL, "engineering_change_id" UUID NOT NULL, CONSTRAINT "engineering_change_comments_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-5 splitStatements:false
CREATE TABLE "engineering_change_issues" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "engineering_change_id" UUID NOT NULL, "issue_id" UUID NOT NULL, CONSTRAINT "engineering_change_issues_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-6 splitStatements:false
CREATE TABLE "engineering_change_steps" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "acted_at" TIMESTAMP WITH TIME ZONE, "acted_by" UUID, "assignee_id" UUID NOT NULL, "assignee_type" VARCHAR(20) NOT NULL, "engineering_change_id" UUID NOT NULL, "sequence" INTEGER NOT NULL, "status" VARCHAR(20) NOT NULL, "step_type" VARCHAR(30) NOT NULL, CONSTRAINT "engineering_change_steps_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-7 splitStatements:false
CREATE TABLE "engineering_changes" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT, "closed_at" TIMESTAMP WITH TIME ZONE, "number" INTEGER NOT NULL, "released_at" TIMESTAMP WITH TIME ZONE, "released_by" UUID, "source_issue_id" UUID, "state" VARCHAR(20) NOT NULL, "title" VARCHAR(500) NOT NULL, CONSTRAINT "engineering_changes_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-8 splitStatements:false
CREATE TABLE "files" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "content_hash" VARCHAR(64), "content_type" VARCHAR(100) NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "file_key" VARCHAR(1000) NOT NULL, "file_size" BIGINT NOT NULL, "original_name" VARCHAR(500) NOT NULL, "owner_id" UUID, "owner_type" VARCHAR(50), "status" VARCHAR(20) NOT NULL, CONSTRAINT "files_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-9 splitStatements:false
CREATE TABLE "histories" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action" VARCHAR(50) NOT NULL, "actor_id" UUID NOT NULL, "detail" JSONB, "target_id" UUID NOT NULL, "target_type" VARCHAR(20) NOT NULL, CONSTRAINT "histories_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-10 splitStatements:false
CREATE TABLE "issue_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "issue_assignees_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-11 splitStatements:false
CREATE TABLE "issue_comments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT NOT NULL, "issue_id" UUID NOT NULL, CONSTRAINT "issue_comments_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-12 splitStatements:false
CREATE TABLE "issue_labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "label_id" UUID NOT NULL, CONSTRAINT "issue_labels_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-13 splitStatements:false
CREATE TABLE "issue_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "part_id" UUID NOT NULL, CONSTRAINT "issue_parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-14 splitStatements:false
CREATE TABLE "issue_team_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "issue_team_assignees_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-15 splitStatements:false
CREATE TABLE "issues" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT, "closed_at" TIMESTAMP WITH TIME ZONE, "number" INTEGER NOT NULL, "state" VARCHAR(20) NOT NULL, "title" VARCHAR(500) NOT NULL, CONSTRAINT "issues_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-16 splitStatements:false
CREATE TABLE "mapping_records" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "name" VARCHAR(200) NOT NULL, "usage_count" INTEGER NOT NULL, CONSTRAINT "mapping_records_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-17 splitStatements:false
CREATE TABLE "mapping_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "file_id" UUID NOT NULL, "mapping" JSONB NOT NULL, "original_headers" JSONB NOT NULL, "record_id" UUID NOT NULL, "sheet_name" VARCHAR(200), "usage_count" INTEGER NOT NULL, "version" INTEGER NOT NULL, CONSTRAINT "mapping_revisions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-18 splitStatements:false
CREATE TABLE "notifications" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "actor_id" UUID NOT NULL, "payload" JSONB NOT NULL, "read_at" TIMESTAMP WITH TIME ZONE, "type" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "notifications_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-19 splitStatements:false
CREATE TABLE "part_revision_histories" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action_type" VARCHAR(30) NOT NULL, "actor_id" UUID, "occurred_at" TIMESTAMP WITH TIME ZONE NOT NULL, "part_revision_id" UUID NOT NULL, "payload" JSONB NOT NULL, "source_ref_id" UUID, "source_type" VARCHAR(30) NOT NULL, CONSTRAINT "part_revision_histories_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-20 splitStatements:false
CREATE TABLE "part_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "base_revision_id" UUID, "category" VARCHAR(100), "description" TEXT, "engineering_change_id" UUID, "extended_properties" JSONB NOT NULL, "lead_time_days" INTEGER, "material" VARCHAR(200), "name" VARCHAR(500), "part_id" UUID NOT NULL, "part_number" VARCHAR(100) NOT NULL, "is_phantom" BOOLEAN, "revision_code" VARCHAR(50), "status" VARCHAR(30) NOT NULL, "unit" VARCHAR(20), CONSTRAINT "part_revisions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-21 splitStatements:false
CREATE TABLE "part_suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "extended_properties" JSONB NOT NULL, "part_revision_id" UUID NOT NULL, "supplier_id" UUID NOT NULL, "unit_cost" FLOAT8, CONSTRAINT "part_suppliers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-22 splitStatements:false
CREATE TABLE "project_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "project_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "project_members_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-23 splitStatements:false
CREATE TABLE "project_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "part_id" UUID NOT NULL, "project_id" UUID NOT NULL, CONSTRAINT "project_parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-24 splitStatements:false
CREATE TABLE "property_definitions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "description" TEXT, "display_name" VARCHAR(200) NOT NULL, "display_order" INTEGER NOT NULL, "option_mode" VARCHAR(20), "options_json" JSONB NOT NULL, "owner_type" VARCHAR(50) NOT NULL, "is_required" BOOLEAN NOT NULL, "value_type" VARCHAR(20) NOT NULL, CONSTRAINT "property_definitions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-25 splitStatements:false
CREATE TABLE "suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "code" VARCHAR(100), "company_name" VARCHAR(200) NOT NULL, "contact_info" TEXT, "country" VARCHAR(100), "extended_properties" JSONB NOT NULL, CONSTRAINT "suppliers_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-26 splitStatements:false
CREATE TABLE "synthesis_batches" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "accepted_count" INTEGER NOT NULL, "failed_uploads" JSONB NOT NULL, "mapping_id" UUID NOT NULL, "project_id" UUID, "requested_by" UUID NOT NULL, "requested_count" INTEGER NOT NULL, CONSTRAINT "synthesis_batches_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-27 splitStatements:false
CREATE TABLE "synthesis_jobs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "batch_id" UUID NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "errors" JSONB NOT NULL, "file_id" UUID NOT NULL, "mapping_id" UUID NOT NULL, "nodes_created" INTEGER NOT NULL, "processed_rows" INTEGER NOT NULL, "relationships_created" INTEGER NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(20) NOT NULL, "total_rows" INTEGER NOT NULL, CONSTRAINT "synthesis_jobs_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-28 splitStatements:false
CREATE TABLE "system_property_overrides" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "display_name_override" VARCHAR(200), "display_order" INTEGER, "owner_type" VARCHAR(50) NOT NULL, "property_key" VARCHAR(100) NOT NULL, CONSTRAINT "system_property_overrides_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-29 splitStatements:false
CREATE TABLE "team_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "user_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "team_members_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-30 splitStatements:false
CREATE TABLE "drawing_artifacts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "artifact_type" VARCHAR(50) NOT NULL, "content_type" VARCHAR(100), "file_id" UUID, "file_size" BIGINT NOT NULL, "format" VARCHAR(30), "published_at" TIMESTAMP WITH TIME ZONE NOT NULL, "storage_key" VARCHAR(1000) NOT NULL, "drawing_id" UUID NOT NULL, CONSTRAINT "drawing_artifacts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-31 splitStatements:false
CREATE TABLE "part_preview_artifacts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "artifact_type" VARCHAR(50) NOT NULL, "content_type" VARCHAR(100), "file_id" UUID, "file_size" BIGINT NOT NULL, "format" VARCHAR(30), "published_at" TIMESTAMP WITH TIME ZONE NOT NULL, "storage_key" VARCHAR(1000) NOT NULL, "part_preview_id" UUID NOT NULL, CONSTRAINT "part_preview_artifacts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-32 splitStatements:false
CREATE TABLE "part_preview_processing_jobs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "attempt_count" INTEGER NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "failure_reason" TEXT, "part_preview_id" UUID NOT NULL, "pipeline_key" VARCHAR(100) NOT NULL, "profile_key" VARCHAR(100) NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(30) NOT NULL, CONSTRAINT "part_preview_processing_jobs_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-33 splitStatements:false
CREATE TABLE "part_previews" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "current_job_id" UUID, "dimension" VARCHAR(30), "part_revision_id" UUID NOT NULL, "conversion_status" VARCHAR(30), "source_id" UUID, "source_type" VARCHAR(20), CONSTRAINT "part_previews_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-34 splitStatements:false
CREATE TABLE "part_revision_workflow_policies" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "mode" VARCHAR(50) NOT NULL, "policy_key" VARCHAR(50) NOT NULL, CONSTRAINT "part_revision_workflow_policies_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-35 splitStatements:false
CREATE TABLE "parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "current_released_revision_id" UUID, "lifecycle_state" VARCHAR(50), "part_number" VARCHAR(100) NOT NULL, CONSTRAINT "parts_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-36 splitStatements:false
CREATE TABLE "labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "color" VARCHAR(7) NOT NULL, "description" VARCHAR(200), "name" VARCHAR(50) NOT NULL, CONSTRAINT "labels_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-37 splitStatements:false
CREATE TABLE "part_preview_files" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "file_id" UUID NOT NULL, "part_preview_id" UUID NOT NULL, CONSTRAINT "part_preview_files_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-38 splitStatements:false
CREATE TABLE "part_preview_serving_projections" ("part_preview_id" UUID NOT NULL, "glb_key" VARCHAR(1000), "pdf_key" VARCHAR(1000), "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "webp_key" VARCHAR(1000), CONSTRAINT "part_preview_serving_projections_pkey" PRIMARY KEY ("part_preview_id"));

-- changeset seongha.moon:1774060140187-39 splitStatements:false
CREATE TABLE "projects" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "is_deleted" BOOLEAN NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "deleted_by" UUID, "is_archived" BOOLEAN NOT NULL, "description" TEXT, "name" VARCHAR(200) NOT NULL, CONSTRAINT "projects_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-40 splitStatements:false
CREATE TABLE "teams" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "description" TEXT, "name" VARCHAR(100) NOT NULL, CONSTRAINT "teams_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-41 splitStatements:false
CREATE TABLE "work_item_number_sequences" ("id" UUID NOT NULL, "next_number" INTEGER NOT NULL, CONSTRAINT "work_item_number_sequences_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1774060140187-42 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id" ON "ai_usage_events" USING btree("org_id");

-- changeset seongha.moon:1774060140187-43 splitStatements:false
CREATE INDEX "ix_ai_usage_events_user_id" ON "ai_usage_events" USING btree("user_id");

-- changeset seongha.moon:1774060140187-44 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id_created_at" ON "ai_usage_events" USING btree("org_id", "created_at");

-- changeset seongha.moon:1774060140187-45 splitStatements:false
CREATE INDEX "ix_drawings_part_revision_id" ON "drawings" USING btree("part_revision_id");

-- changeset seongha.moon:1774060140187-46 splitStatements:false
ALTER TABLE "drawings" ADD CONSTRAINT "uq_drawings_drawing_number" UNIQUE ("drawing_number");

-- changeset seongha.moon:1774060140187-47 splitStatements:false
CREATE INDEX "ix_engineering_bom_items_parent_part_revision_id" ON "engineering_bom_items" USING btree("parent_part_revision_id");

-- changeset seongha.moon:1774060140187-48 splitStatements:false
CREATE INDEX "ix_engineering_bom_items_child_part_revision_id" ON "engineering_bom_items" USING btree("child_part_revision_id");

-- changeset seongha.moon:1774060140187-49 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "uq_engineering_bom_items_parent_revision_line_number" UNIQUE ("parent_part_revision_id", "line_number");

-- changeset seongha.moon:1774060140187-50 splitStatements:false
CREATE INDEX "ix_engineering_change_comments_engineering_change_id" ON "engineering_change_comments" USING btree("engineering_change_id");

-- changeset seongha.moon:1774060140187-51 splitStatements:false
CREATE INDEX "ix_engineering_change_issues_engineering_change_id" ON "engineering_change_issues" USING btree("engineering_change_id");

-- changeset seongha.moon:1774060140187-52 splitStatements:false
CREATE INDEX "ix_engineering_change_issues_issue_id" ON "engineering_change_issues" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-53 splitStatements:false
ALTER TABLE "engineering_change_issues" ADD CONSTRAINT "uq_engineering_change_issues_engineering_change_id_issue_id" UNIQUE ("engineering_change_id", "issue_id");

-- changeset seongha.moon:1774060140187-54 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_engineering_change_id" ON "engineering_change_steps" USING btree("engineering_change_id");

-- changeset seongha.moon:1774060140187-55 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_step_type" ON "engineering_change_steps" USING btree("step_type");

-- changeset seongha.moon:1774060140187-56 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_assignee_id" ON "engineering_change_steps" USING btree("assignee_id");

-- changeset seongha.moon:1774060140187-57 splitStatements:false
CREATE INDEX "ix_engineering_changes_state" ON "engineering_changes" USING btree("state");

-- changeset seongha.moon:1774060140187-58 splitStatements:false
CREATE INDEX "ix_engineering_changes_created_by" ON "engineering_changes" USING btree("created_by");

-- changeset seongha.moon:1774060140187-59 splitStatements:false
ALTER TABLE "engineering_changes" ADD CONSTRAINT "uq_engineering_changes_number" UNIQUE ("number");

-- changeset seongha.moon:1774060140187-60 splitStatements:false
CREATE INDEX "ix_files_owner_type_owner_id" ON "files" USING btree("owner_type", "owner_id");

-- changeset seongha.moon:1774060140187-61 splitStatements:false
CREATE INDEX "ix_files_original_name_file_size_content_hash" ON "files" USING btree("original_name", "file_size", "content_hash");

-- changeset seongha.moon:1774060140187-62 splitStatements:false
ALTER TABLE "files" ADD CONSTRAINT "uq_files_file_key" UNIQUE ("file_key");

-- changeset seongha.moon:1774060140187-63 splitStatements:false
CREATE INDEX "ix_activities_target" ON "histories" USING btree("target_type", "target_id");

-- changeset seongha.moon:1774060140187-64 splitStatements:false
CREATE INDEX "ix_issue_assignees_issue_id" ON "issue_assignees" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-65 splitStatements:false
CREATE INDEX "ix_issue_assignees_user_id" ON "issue_assignees" USING btree("user_id");

-- changeset seongha.moon:1774060140187-66 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "uq_issue_assignees_issue_id_user_id" UNIQUE ("issue_id", "user_id");

-- changeset seongha.moon:1774060140187-67 splitStatements:false
CREATE INDEX "ix_issue_comments_issue_id" ON "issue_comments" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-68 splitStatements:false
CREATE INDEX "ix_issue_labels_issue_id" ON "issue_labels" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-69 splitStatements:false
CREATE INDEX "ix_issue_labels_label_id" ON "issue_labels" USING btree("label_id");

-- changeset seongha.moon:1774060140187-70 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "uq_issue_labels_issue_id_label_id" UNIQUE ("issue_id", "label_id");

-- changeset seongha.moon:1774060140187-71 splitStatements:false
CREATE INDEX "ix_issue_parts_issue_id" ON "issue_parts" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-72 splitStatements:false
CREATE INDEX "ix_issue_parts_part_id" ON "issue_parts" USING btree("part_id");

-- changeset seongha.moon:1774060140187-73 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "uq_issue_parts_issue_id_part_id" UNIQUE ("issue_id", "part_id");

-- changeset seongha.moon:1774060140187-74 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_issue_id" ON "issue_team_assignees" USING btree("issue_id");

-- changeset seongha.moon:1774060140187-75 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_team_id" ON "issue_team_assignees" USING btree("team_id");

-- changeset seongha.moon:1774060140187-76 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "uq_issue_team_assignees_issue_id_team_id" UNIQUE ("issue_id", "team_id");

-- changeset seongha.moon:1774060140187-77 splitStatements:false
CREATE INDEX "ix_issues_state" ON "issues" USING btree("state");

-- changeset seongha.moon:1774060140187-78 splitStatements:false
CREATE INDEX "ix_issues_created_by" ON "issues" USING btree("created_by");

-- changeset seongha.moon:1774060140187-79 splitStatements:false
ALTER TABLE "issues" ADD CONSTRAINT "uq_issues_number" UNIQUE ("number");

-- changeset seongha.moon:1774060140187-80 splitStatements:false
CREATE INDEX "ix_mapping_records_is_active" ON "mapping_records" USING btree("is_active");

-- changeset seongha.moon:1774060140187-81 splitStatements:false
ALTER TABLE "mapping_records" ADD CONSTRAINT "uq_mapping_records_name" UNIQUE ("name");

-- changeset seongha.moon:1774060140187-82 splitStatements:false
CREATE INDEX "ix_mapping_revisions_record_id" ON "mapping_revisions" USING btree("record_id");

-- changeset seongha.moon:1774060140187-83 splitStatements:false
CREATE INDEX "ix_mapping_revisions_file_id" ON "mapping_revisions" USING btree("file_id");

-- changeset seongha.moon:1774060140187-84 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "uq_mapping_revisions_record_version" UNIQUE ("record_id", "version");

-- changeset seongha.moon:1774060140187-85 splitStatements:false
CREATE INDEX "ix_notifications_user_unread" ON "notifications" USING btree("user_id", "read_at");

-- changeset seongha.moon:1774060140187-86 splitStatements:false
CREATE INDEX "ix_part_revision_histories_revision_created" ON "part_revision_histories" USING btree("part_revision_id", "created_at");

-- changeset seongha.moon:1774060140187-87 splitStatements:false
CREATE INDEX "ix_part_revision_histories_actor_id" ON "part_revision_histories" USING btree("actor_id");

-- changeset seongha.moon:1774060140187-88 splitStatements:false
CREATE INDEX "ix_part_revision_histories_source_ref_id" ON "part_revision_histories" USING btree("source_ref_id");

-- changeset seongha.moon:1774060140187-89 splitStatements:false
CREATE INDEX "ix_part_revisions_part_id" ON "part_revisions" USING btree("part_id");

-- changeset seongha.moon:1774060140187-90 splitStatements:false
CREATE INDEX "ix_part_revisions_part_number" ON "part_revisions" USING btree("part_number");

-- changeset seongha.moon:1774060140187-91 splitStatements:false
CREATE INDEX "ix_part_revisions_base_revision_id" ON "part_revisions" USING btree("base_revision_id");

-- changeset seongha.moon:1774060140187-92 splitStatements:false
CREATE INDEX "ix_part_revisions_created_by" ON "part_revisions" USING btree("created_by");

-- changeset seongha.moon:1774060140187-93 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "uq_part_revisions_part_number_revision_code" UNIQUE ("part_number", "revision_code");

-- changeset seongha.moon:1774060140187-94 splitStatements:false
CREATE INDEX "ix_part_suppliers_part_revision_id" ON "part_suppliers" USING btree("part_revision_id");

-- changeset seongha.moon:1774060140187-95 splitStatements:false
CREATE INDEX "ix_part_suppliers_supplier_id" ON "part_suppliers" USING btree("supplier_id");

-- changeset seongha.moon:1774060140187-96 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "uq_part_suppliers_part_revision_id_supplier_id" UNIQUE ("part_revision_id", "supplier_id");

-- changeset seongha.moon:1774060140187-97 splitStatements:false
CREATE INDEX "ix_project_members_project_id" ON "project_members" USING btree("project_id");

-- changeset seongha.moon:1774060140187-98 splitStatements:false
CREATE INDEX "ix_project_members_user_id" ON "project_members" USING btree("user_id");

-- changeset seongha.moon:1774060140187-99 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "uq_project_members_project_id_user_id" UNIQUE ("project_id", "user_id");

-- changeset seongha.moon:1774060140187-100 splitStatements:false
CREATE INDEX "ix_project_parts_project_id" ON "project_parts" USING btree("project_id");

-- changeset seongha.moon:1774060140187-101 splitStatements:false
CREATE INDEX "ix_project_parts_part_id" ON "project_parts" USING btree("part_id");

-- changeset seongha.moon:1774060140187-102 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "uq_project_parts_project_id_part_id" UNIQUE ("project_id", "part_id");

-- changeset seongha.moon:1774060140187-103 splitStatements:false
CREATE INDEX "ix_property_definitions_owner_type_is_active_display_order" ON "property_definitions" USING btree("owner_type", "is_active", "display_order");

-- changeset seongha.moon:1774060140187-104 splitStatements:false
ALTER TABLE "property_definitions" ADD CONSTRAINT "uq_property_definitions_owner_type_display_name" UNIQUE ("owner_type", "display_name");

-- changeset seongha.moon:1774060140187-105 splitStatements:false
CREATE INDEX "ix_suppliers_code" ON "suppliers" USING btree("code");

-- changeset seongha.moon:1774060140187-106 splitStatements:false
ALTER TABLE "suppliers" ADD CONSTRAINT "uq_suppliers_company_name" UNIQUE ("company_name");

-- changeset seongha.moon:1774060140187-107 splitStatements:false
CREATE INDEX "ix_synthesis_batches_project_id" ON "synthesis_batches" USING btree("project_id");

-- changeset seongha.moon:1774060140187-108 splitStatements:false
CREATE INDEX "ix_synthesis_batches_mapping_id" ON "synthesis_batches" USING btree("mapping_id");

-- changeset seongha.moon:1774060140187-109 splitStatements:false
CREATE INDEX "ix_synthesis_batches_requested_by" ON "synthesis_batches" USING btree("requested_by");

-- changeset seongha.moon:1774060140187-110 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_batch_id" ON "synthesis_jobs" USING btree("batch_id");

-- changeset seongha.moon:1774060140187-111 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_mapping_id" ON "synthesis_jobs" USING btree("mapping_id");

-- changeset seongha.moon:1774060140187-112 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_file_id" ON "synthesis_jobs" USING btree("file_id");

-- changeset seongha.moon:1774060140187-113 splitStatements:false
CREATE INDEX "ix_system_property_overrides_owner_type_is_active_display_order" ON "system_property_overrides" USING btree("owner_type", "is_active", "display_order");

-- changeset seongha.moon:1774060140187-114 splitStatements:false
ALTER TABLE "system_property_overrides" ADD CONSTRAINT "uq_system_property_overrides_owner_type_property_key" UNIQUE ("owner_type", "property_key");

-- changeset seongha.moon:1774060140187-115 splitStatements:false
CREATE INDEX "ix_team_members_team_id" ON "team_members" USING btree("team_id");

-- changeset seongha.moon:1774060140187-116 splitStatements:false
CREATE INDEX "ix_team_members_user_id" ON "team_members" USING btree("user_id");

-- changeset seongha.moon:1774060140187-117 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "uq_team_members_team_id_user_id" UNIQUE ("team_id", "user_id");

-- changeset seongha.moon:1774060140187-118 splitStatements:false
ALTER TABLE "drawing_artifacts" ADD CONSTRAINT "uq_drawing_artifacts_drawing_type" UNIQUE ("drawing_id", "artifact_type");

-- changeset seongha.moon:1774060140187-119 splitStatements:false
ALTER TABLE "part_preview_artifacts" ADD CONSTRAINT "uq_part_preview_artifacts_preview_type" UNIQUE ("part_preview_id", "artifact_type");

-- changeset seongha.moon:1774060140187-120 splitStatements:false
ALTER TABLE "part_previews" ADD CONSTRAINT "uq_part_previews_part_revision_id" UNIQUE ("part_revision_id");

-- changeset seongha.moon:1774060140187-121 splitStatements:false
ALTER TABLE "part_revision_workflow_policies" ADD CONSTRAINT "uq_part_revision_workflow_policies_policy_key" UNIQUE ("policy_key");

-- changeset seongha.moon:1774060140187-122 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "uq_parts_part_number" UNIQUE ("part_number");

-- changeset seongha.moon:1774060140187-123 splitStatements:false
ALTER TABLE "labels" ADD CONSTRAINT "uq_labels_name" UNIQUE ("name");

-- changeset seongha.moon:1774060140187-124 splitStatements:false
ALTER TABLE "part_preview_files" ADD CONSTRAINT "uq_part_preview_files_file_id" UNIQUE ("file_id");

-- changeset seongha.moon:1774060140187-125 splitStatements:false
ALTER TABLE "teams" ADD CONSTRAINT "uq_teams_name" UNIQUE ("name");

-- changeset seongha.moon:1774060140187-126 splitStatements:false
ALTER TABLE "engineering_change_comments" ADD CONSTRAINT "fk1g95iugo186w07u6gawmanhdr" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-127 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk2w9t9pm4a18rihij6xcup7kun" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-128 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fk4brv9b343p36yict83fyfixdy" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-129 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "fk56bv9knlohikvliauqx0s6x35" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-130 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk5ddsht7he9hab9xu47wnook3f" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-131 splitStatements:false
ALTER TABLE "engineering_change_issues" ADD CONSTRAINT "fk5i2rg8agkl18atn0jbjaxr8ol" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-132 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "fk81q8jhewj6k8k5s1y4cw1wdyo" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-133 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk_mapping_revisions_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-134 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk_mapping_revisions_record_id" FOREIGN KEY ("record_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-135 splitStatements:false
ALTER TABLE "part_revision_histories" ADD CONSTRAINT "fk_part_revision_histories_part_revision_id" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-136 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-137 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_project_id" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-138 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-139 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-140 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "fk_team_members_team_id" FOREIGN KEY ("team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-141 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "fkayul0e21mxb13ncdi8kae6obr" FOREIGN KEY ("child_part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-142 splitStatements:false
ALTER TABLE "part_previews" ADD CONSTRAINT "fkbvtyw5qsiaevxfqwdonbc242x" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-143 splitStatements:false
ALTER TABLE "drawing_artifacts" ADD CONSTRAINT "fkcvt2rdwm199odunwkj17i1k8n" FOREIGN KEY ("drawing_id") REFERENCES "drawings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-144 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fkd47ukwxcpdb87h3b6kd017k9n" FOREIGN KEY ("supplier_id") REFERENCES "suppliers" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-145 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "fkdki1sp2homqsdcvqm9yrix31g" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-146 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "fkeee47ncsdhj0917jfcjnrbrvv" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-147 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "fkelfms0mcqge2jlvp4podgjunj" FOREIGN KEY ("parent_part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-148 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fkf4vylyr6m7ofb2jd28fla6ry7" FOREIGN KEY ("batch_id") REFERENCES "synthesis_batches" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-149 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "fkfskpaxuixega1rlf9hrhnhngs" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-150 splitStatements:false
ALTER TABLE "part_preview_files" ADD CONSTRAINT "fkhovn2awghkyvs82v0jblam02p" FOREIGN KEY ("part_preview_id") REFERENCES "part_previews" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-151 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fki6fs3anwh4t24809xn23xilu4" FOREIGN KEY ("base_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-152 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fkl1cfng9egr5y8m0chwm32y9di" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-153 splitStatements:false
ALTER TABLE "part_preview_artifacts" ADD CONSTRAINT "fklauu1wqvbrynnfd8v9r0hquj9" FOREIGN KEY ("part_preview_id") REFERENCES "part_previews" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-154 splitStatements:false
ALTER TABLE "engineering_change_steps" ADD CONSTRAINT "fkm9g6yiohm3rlx0qv3gr1y0040" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-155 splitStatements:false
ALTER TABLE "issue_comments" ADD CONSTRAINT "fknvnj0204928o0w1th5jsx4f28" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-156 splitStatements:false
ALTER TABLE "drawings" ADD CONSTRAINT "fkovfe3bykl1mphod4f8odh26oc" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1774060140187-157 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "fkqewjubof3sw38w93c20x4j4xf" FOREIGN KEY ("current_released_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

