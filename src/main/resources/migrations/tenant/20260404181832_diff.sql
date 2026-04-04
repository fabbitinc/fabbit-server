-- liquibase formatted sql

-- changeset moonseongha:1775294317245-1 splitStatements:false
CREATE TABLE "ai_usage_events" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "billable_amount" numeric(12, 2) NOT NULL, "billing_status" VARCHAR(20) NOT NULL, "category" VARCHAR(30) NOT NULL, "credits_used" numeric(12, 4) NOT NULL, "feature" VARCHAR(50) NOT NULL, "input_tokens" INTEGER NOT NULL, "metadata" JSONB NOT NULL, "model" VARCHAR(50) NOT NULL, "org_id" UUID NOT NULL, "output_tokens" INTEGER NOT NULL, "plan_type_snapshot" VARCHAR(20) NOT NULL, "seat_type_snapshot" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "ai_usage_events_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-2 splitStatements:false
CREATE TABLE "chat_action_requests" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action_type" VARCHAR(50) NOT NULL, "confirmed_at" TIMESTAMP WITH TIME ZONE, "confirmed_by" UUID, "executed_at" TIMESTAMP WITH TIME ZONE, "expires_at" TIMESTAMP WITH TIME ZONE, "preview_payload" JSONB NOT NULL, "request_payload" JSONB NOT NULL, "result_payload" JSONB NOT NULL, "run_id" UUID NOT NULL, "status" VARCHAR(20) NOT NULL, "thread_id" UUID NOT NULL, CONSTRAINT "chat_action_requests_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-3 splitStatements:false
CREATE TABLE "chat_messages" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "content" JSONB NOT NULL, "message_type" VARCHAR(20) NOT NULL, "role" VARCHAR(20) NOT NULL, "run_id" UUID, "sequence" BIGINT NOT NULL, "status" VARCHAR(20) NOT NULL, "thread_id" UUID NOT NULL, CONSTRAINT "chat_messages_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-4 splitStatements:false
CREATE TABLE "chat_run_events" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "event_type" VARCHAR(50) NOT NULL, "payload" JSONB NOT NULL, "run_id" UUID NOT NULL, "sequence" BIGINT NOT NULL, "visibility" VARCHAR(20) NOT NULL, CONSTRAINT "chat_run_events_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-5 splitStatements:false
CREATE TABLE "chat_runs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "assistant_message_id" UUID, "completed_at" TIMESTAMP WITH TIME ZONE, "error_code" VARCHAR(100), "input_tokens" INTEGER NOT NULL, "intent" VARCHAR(50) NOT NULL, "metadata" JSONB NOT NULL, "model" VARCHAR(100) NOT NULL, "output_tokens" INTEGER NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(30) NOT NULL, "thread_id" UUID NOT NULL, "user_message_id" UUID NOT NULL, CONSTRAINT "chat_runs_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-6 splitStatements:false
CREATE TABLE "chat_threads" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "context_id" UUID, "context_type" VARCHAR(30) NOT NULL, "last_message_at" TIMESTAMP WITH TIME ZONE NOT NULL, "org_id" UUID NOT NULL, "project_id" UUID, "status" VARCHAR(20) NOT NULL, "title" VARCHAR(200) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "chat_threads_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-7 splitStatements:false
CREATE TABLE "chat_tool_calls" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "arguments_json" JSONB NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "error_code" VARCHAR(100), "result_json" JSONB NOT NULL, "run_id" UUID NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE NOT NULL, "status" VARCHAR(20) NOT NULL, "thread_id" UUID NOT NULL, "tool_name" VARCHAR(100) NOT NULL, CONSTRAINT "chat_tool_calls_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-8 splitStatements:false
CREATE TABLE "drawings" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "dimension" VARCHAR(30), "drawing_number" VARCHAR(100), "name" VARCHAR(500), "original_file_key" VARCHAR(1000), "part_revision_id" UUID, "source_file_id" UUID, "source_type" VARCHAR(30), "status" VARCHAR(50), "version" VARCHAR(50), CONSTRAINT "drawings_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-9 splitStatements:false
CREATE TABLE "engineering_bom_items" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "child_part_revision_id" UUID NOT NULL, "extended_properties" JSONB NOT NULL, "line_number" VARCHAR(50) NOT NULL, "parent_part_revision_id" UUID NOT NULL, "quantity" numeric(19, 6) NOT NULL, CONSTRAINT "engineering_bom_items_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-10 splitStatements:false
CREATE TABLE "engineering_change_affected_items" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action_detail" TEXT, "engineering_change_id" UUID NOT NULL, "item_type" VARCHAR(30) NOT NULL, "target_id" UUID NOT NULL, CONSTRAINT "engineering_change_affected_items_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-11 splitStatements:false
CREATE TABLE "engineering_change_comments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT NOT NULL, "engineering_change_id" UUID NOT NULL, CONSTRAINT "engineering_change_comments_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-12 splitStatements:false
CREATE TABLE "engineering_change_issues" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "engineering_change_id" UUID NOT NULL, "issue_id" UUID NOT NULL, CONSTRAINT "engineering_change_issues_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-13 splitStatements:false
CREATE TABLE "engineering_change_steps" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "acted_at" TIMESTAMP WITH TIME ZONE, "acted_by" UUID, "assignee_id" UUID NOT NULL, "assignee_type" VARCHAR(20) NOT NULL, "engineering_change_id" UUID NOT NULL, "sequence" INTEGER NOT NULL, "status" VARCHAR(20) NOT NULL, "step_type" VARCHAR(30) NOT NULL, CONSTRAINT "engineering_change_steps_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-14 splitStatements:false
CREATE TABLE "engineering_changes" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT, "closed_at" TIMESTAMP WITH TIME ZONE, "number" INTEGER NOT NULL, "released_at" TIMESTAMP WITH TIME ZONE, "released_by" UUID, "source_issue_id" UUID, "state" VARCHAR(20) NOT NULL, "title" VARCHAR(500) NOT NULL, CONSTRAINT "engineering_changes_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-15 splitStatements:false
CREATE TABLE "files" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "content_hash" VARCHAR(64), "content_type" VARCHAR(100) NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "file_key" VARCHAR(1000) NOT NULL, "file_size" BIGINT NOT NULL, "original_name" VARCHAR(500) NOT NULL, "owner_id" UUID, "owner_type" VARCHAR(50), "status" VARCHAR(20) NOT NULL, CONSTRAINT "files_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-16 splitStatements:false
CREATE TABLE "histories" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action" VARCHAR(50) NOT NULL, "actor_id" UUID NOT NULL, "detail" JSONB, "target_id" UUID NOT NULL, "target_type" VARCHAR(20) NOT NULL, CONSTRAINT "histories_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-17 splitStatements:false
CREATE TABLE "issue_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "issue_assignees_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-18 splitStatements:false
CREATE TABLE "issue_comments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT NOT NULL, "issue_id" UUID NOT NULL, CONSTRAINT "issue_comments_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-19 splitStatements:false
CREATE TABLE "issue_labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "label_id" UUID NOT NULL, CONSTRAINT "issue_labels_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-20 splitStatements:false
CREATE TABLE "issue_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "part_id" UUID NOT NULL, CONSTRAINT "issue_parts_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-21 splitStatements:false
CREATE TABLE "issue_team_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "issue_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "issue_team_assignees_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-22 splitStatements:false
CREATE TABLE "issues" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "body" TEXT, "closed_at" TIMESTAMP WITH TIME ZONE, "number" INTEGER NOT NULL, "state" VARCHAR(20) NOT NULL, "title" VARCHAR(500) NOT NULL, CONSTRAINT "issues_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-23 splitStatements:false
CREATE TABLE "mapping_records" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "name" VARCHAR(200) NOT NULL, "usage_count" INTEGER NOT NULL, CONSTRAINT "mapping_records_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-24 splitStatements:false
CREATE TABLE "mapping_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "file_id" UUID NOT NULL, "mapping" JSONB NOT NULL, "original_headers" JSONB NOT NULL, "record_id" UUID NOT NULL, "sheet_name" VARCHAR(200), "usage_count" INTEGER NOT NULL, "version" INTEGER NOT NULL, CONSTRAINT "mapping_revisions_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-25 splitStatements:false
CREATE TABLE "notifications" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "actor_id" UUID NOT NULL, "payload" JSONB NOT NULL, "read_at" TIMESTAMP WITH TIME ZONE, "type" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "notifications_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-26 splitStatements:false
CREATE TABLE "part_revision_histories" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "action_type" VARCHAR(30) NOT NULL, "actor_id" UUID, "occurred_at" TIMESTAMP WITH TIME ZONE NOT NULL, "part_revision_id" UUID NOT NULL, "payload" JSONB NOT NULL, "source_ref_id" UUID, "source_type" VARCHAR(30) NOT NULL, CONSTRAINT "part_revision_histories_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-27 splitStatements:false
CREATE TABLE "part_revisions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "base_revision_id" UUID, "description" TEXT, "extended_properties" JSONB NOT NULL, "lead_time_days" INTEGER, "material" VARCHAR(200), "name" VARCHAR(500), "part_id" UUID NOT NULL, "part_number" VARCHAR(100) NOT NULL, "revision_code" VARCHAR(50), "status" VARCHAR(30) NOT NULL, "unit" VARCHAR(20), CONSTRAINT "part_revisions_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-28 splitStatements:false
CREATE TABLE "part_suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "extended_properties" JSONB NOT NULL, "part_revision_id" UUID NOT NULL, "supplier_id" UUID NOT NULL, "unit_cost" FLOAT8, CONSTRAINT "part_suppliers_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-29 splitStatements:false
CREATE TABLE "project_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "project_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "project_members_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-30 splitStatements:false
CREATE TABLE "project_parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "part_id" UUID NOT NULL, "project_id" UUID NOT NULL, CONSTRAINT "project_parts_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-31 splitStatements:false
CREATE TABLE "property_definitions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "is_active_configurable" BOOLEAN NOT NULL, "description" TEXT, "display_name" VARCHAR(200) NOT NULL, "display_order" INTEGER NOT NULL, "option_mode" VARCHAR(20), "options_json" JSONB NOT NULL, "owner_type" VARCHAR(50) NOT NULL, "part_system_property_kind" VARCHAR(50), "property_key" VARCHAR(100) NOT NULL, "is_required" BOOLEAN NOT NULL, "source_type" VARCHAR(20) NOT NULL, "storage_binding" VARCHAR(200) NOT NULL, "storage_kind" VARCHAR(30) NOT NULL, "value_type" VARCHAR(20) NOT NULL, CONSTRAINT "property_definitions_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-32 splitStatements:false
CREATE TABLE "suppliers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "code" VARCHAR(100), "company_name" VARCHAR(200) NOT NULL, "contact_info" TEXT, "country" VARCHAR(100), "extended_properties" JSONB NOT NULL, CONSTRAINT "suppliers_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-33 splitStatements:false
CREATE TABLE "synthesis_batches" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "accepted_count" INTEGER NOT NULL, "failed_uploads" JSONB NOT NULL, "mapping_id" UUID NOT NULL, "project_id" UUID, "requested_by" UUID NOT NULL, "requested_count" INTEGER NOT NULL, CONSTRAINT "synthesis_batches_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-34 splitStatements:false
CREATE TABLE "synthesis_jobs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "batch_id" UUID NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "errors" JSONB NOT NULL, "file_id" UUID NOT NULL, "mapping_id" UUID NOT NULL, "nodes_created" INTEGER NOT NULL, "processed_rows" INTEGER NOT NULL, "relationships_created" INTEGER NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(20) NOT NULL, "total_rows" INTEGER NOT NULL, CONSTRAINT "synthesis_jobs_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-35 splitStatements:false
CREATE TABLE "team_members" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "user_id" UUID NOT NULL, "team_id" UUID NOT NULL, CONSTRAINT "team_members_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-36 splitStatements:false
CREATE TABLE "drawing_artifacts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "artifact_type" VARCHAR(50) NOT NULL, "content_type" VARCHAR(100), "file_id" UUID, "file_size" BIGINT NOT NULL, "format" VARCHAR(30), "published_at" TIMESTAMP WITH TIME ZONE NOT NULL, "storage_key" VARCHAR(1000) NOT NULL, "drawing_id" UUID NOT NULL, CONSTRAINT "drawing_artifacts_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-37 splitStatements:false
CREATE TABLE "part_preview_artifacts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "artifact_type" VARCHAR(50) NOT NULL, "content_type" VARCHAR(100), "file_id" UUID, "file_size" BIGINT NOT NULL, "format" VARCHAR(30), "published_at" TIMESTAMP WITH TIME ZONE NOT NULL, "storage_key" VARCHAR(1000) NOT NULL, "part_preview_id" UUID NOT NULL, CONSTRAINT "part_preview_artifacts_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-38 splitStatements:false
CREATE TABLE "part_preview_processing_jobs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "attempt_count" INTEGER NOT NULL, "completed_at" TIMESTAMP WITH TIME ZONE, "failure_reason" TEXT, "part_preview_id" UUID NOT NULL, "pipeline_key" VARCHAR(100) NOT NULL, "profile_key" VARCHAR(100) NOT NULL, "started_at" TIMESTAMP WITH TIME ZONE, "status" VARCHAR(30) NOT NULL, CONSTRAINT "part_preview_processing_jobs_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-39 splitStatements:false
CREATE TABLE "part_previews" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "current_job_id" UUID, "dimension" VARCHAR(30), "part_revision_id" UUID NOT NULL, "conversion_status" VARCHAR(30), "source_id" UUID, "source_type" VARCHAR(20), CONSTRAINT "part_previews_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-40 splitStatements:false
CREATE TABLE "part_revision_workflow_policies" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "mode" VARCHAR(50) NOT NULL, "policy_key" VARCHAR(50) NOT NULL, CONSTRAINT "part_revision_workflow_policies_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-41 splitStatements:false
CREATE TABLE "parts" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "current_released_revision_id" UUID, "item_type" VARCHAR(30), "lifecycle_state" VARCHAR(50) NOT NULL, "numbering_category_id" UUID, "part_number" VARCHAR(100) NOT NULL, CONSTRAINT "parts_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-42 splitStatements:false
CREATE TABLE "labels" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "color" VARCHAR(7) NOT NULL, "description" VARCHAR(200), "name" VARCHAR(50) NOT NULL, CONSTRAINT "labels_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-43 splitStatements:false
CREATE TABLE "part_number_categories" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "delimiter" VARCHAR(5) NOT NULL, "digits" INTEGER NOT NULL, "name" VARCHAR(100) NOT NULL, "prefix" VARCHAR(20) NOT NULL, CONSTRAINT "part_number_categories_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-44 splitStatements:false
CREATE TABLE "part_number_sequences" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "category_id" UUID NOT NULL, "current_value" INTEGER NOT NULL, CONSTRAINT "part_number_sequences_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-45 splitStatements:false
CREATE TABLE "part_preview_files" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "file_id" UUID NOT NULL, "part_preview_id" UUID NOT NULL, CONSTRAINT "part_preview_files_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-46 splitStatements:false
CREATE TABLE "part_preview_serving_projections" ("part_preview_id" UUID NOT NULL, "glb_key" VARCHAR(1000), "pdf_key" VARCHAR(1000), "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "webp_key" VARCHAR(1000), CONSTRAINT "part_preview_serving_projections_pkey" PRIMARY KEY ("part_preview_id"));

-- changeset moonseongha:1775294317245-47 splitStatements:false
CREATE TABLE "projects" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "is_deleted" BOOLEAN NOT NULL, "deleted_at" TIMESTAMP WITH TIME ZONE, "deleted_by" UUID, "is_archived" BOOLEAN NOT NULL, "description" TEXT, "name" VARCHAR(200) NOT NULL, CONSTRAINT "projects_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-48 splitStatements:false
CREATE TABLE "teams" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "description" TEXT, "name" VARCHAR(100) NOT NULL, CONSTRAINT "teams_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-49 splitStatements:false
CREATE TABLE "work_item_number_sequences" ("id" UUID NOT NULL, "next_number" INTEGER NOT NULL, CONSTRAINT "work_item_number_sequences_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775294317245-50 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id" ON "ai_usage_events" USING btree("org_id");

-- changeset moonseongha:1775294317245-51 splitStatements:false
CREATE INDEX "ix_ai_usage_events_user_id" ON "ai_usage_events" USING btree("user_id");

-- changeset moonseongha:1775294317245-52 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id_created_at" ON "ai_usage_events" USING btree("org_id", "created_at");

-- changeset moonseongha:1775294317245-53 splitStatements:false
CREATE INDEX "ix_chat_action_requests_thread_created_at" ON "chat_action_requests" USING btree("thread_id", "created_at");

-- changeset moonseongha:1775294317245-54 splitStatements:false
CREATE INDEX "ix_chat_action_requests_status_expires_at" ON "chat_action_requests" USING btree("status", "expires_at");

-- changeset moonseongha:1775294317245-55 splitStatements:false
CREATE INDEX "ix_chat_messages_thread_sequence" ON "chat_messages" USING btree("thread_id", "sequence");

-- changeset moonseongha:1775294317245-56 splitStatements:false
CREATE INDEX "ix_chat_messages_run_id" ON "chat_messages" USING btree("run_id");

-- changeset moonseongha:1775294317245-57 splitStatements:false
CREATE INDEX "ix_chat_run_events_run_sequence" ON "chat_run_events" USING btree("run_id", "sequence");

-- changeset moonseongha:1775294317245-58 splitStatements:false
CREATE INDEX "ix_chat_runs_thread_created_at" ON "chat_runs" USING btree("thread_id", "created_at");

-- changeset moonseongha:1775294317245-59 splitStatements:false
CREATE INDEX "ix_chat_runs_status_created_at" ON "chat_runs" USING btree("status", "created_at");

-- changeset moonseongha:1775294317245-60 splitStatements:false
CREATE INDEX "ix_chat_threads_org_user_created_at" ON "chat_threads" USING btree("org_id", "user_id", "created_at");

-- changeset moonseongha:1775294317245-61 splitStatements:false
CREATE INDEX "ix_chat_threads_org_context" ON "chat_threads" USING btree("org_id", "context_type", "context_id");

-- changeset moonseongha:1775294317245-62 splitStatements:false
CREATE INDEX "ix_chat_threads_org_last_message_at" ON "chat_threads" USING btree("org_id", "last_message_at");

-- changeset moonseongha:1775294317245-63 splitStatements:false
CREATE INDEX "ix_chat_tool_calls_run_created_at" ON "chat_tool_calls" USING btree("run_id", "created_at");

-- changeset moonseongha:1775294317245-64 splitStatements:false
CREATE INDEX "ix_chat_tool_calls_tool_status_created_at" ON "chat_tool_calls" USING btree("tool_name", "status", "created_at");

-- changeset moonseongha:1775294317245-65 splitStatements:false
CREATE INDEX "ix_drawings_part_revision_id" ON "drawings" USING btree("part_revision_id");

-- changeset moonseongha:1775294317245-66 splitStatements:false
ALTER TABLE "drawings" ADD CONSTRAINT "uq_drawings_drawing_number" UNIQUE ("drawing_number");

-- changeset moonseongha:1775294317245-67 splitStatements:false
CREATE INDEX "ix_engineering_bom_items_parent_part_revision_id" ON "engineering_bom_items" USING btree("parent_part_revision_id");

-- changeset moonseongha:1775294317245-68 splitStatements:false
CREATE INDEX "ix_engineering_bom_items_child_part_revision_id" ON "engineering_bom_items" USING btree("child_part_revision_id");

-- changeset moonseongha:1775294317245-69 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "uq_engineering_bom_items_parent_revision_line_number" UNIQUE ("parent_part_revision_id", "line_number");

-- changeset moonseongha:1775294317245-70 splitStatements:false
CREATE INDEX "ix_ec_affected_items_ec_id" ON "engineering_change_affected_items" USING btree("engineering_change_id");

-- changeset moonseongha:1775294317245-71 splitStatements:false
CREATE INDEX "ix_ec_affected_items_target_id" ON "engineering_change_affected_items" USING btree("target_id");

-- changeset moonseongha:1775294317245-72 splitStatements:false
CREATE INDEX "ix_engineering_change_comments_engineering_change_id" ON "engineering_change_comments" USING btree("engineering_change_id");

-- changeset moonseongha:1775294317245-73 splitStatements:false
CREATE INDEX "ix_engineering_change_issues_engineering_change_id" ON "engineering_change_issues" USING btree("engineering_change_id");

-- changeset moonseongha:1775294317245-74 splitStatements:false
CREATE INDEX "ix_engineering_change_issues_issue_id" ON "engineering_change_issues" USING btree("issue_id");

-- changeset moonseongha:1775294317245-75 splitStatements:false
ALTER TABLE "engineering_change_issues" ADD CONSTRAINT "uq_engineering_change_issues_engineering_change_id_issue_id" UNIQUE ("engineering_change_id", "issue_id");

-- changeset moonseongha:1775294317245-76 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_engineering_change_id" ON "engineering_change_steps" USING btree("engineering_change_id");

-- changeset moonseongha:1775294317245-77 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_step_type" ON "engineering_change_steps" USING btree("step_type");

-- changeset moonseongha:1775294317245-78 splitStatements:false
CREATE INDEX "ix_engineering_change_steps_assignee_id" ON "engineering_change_steps" USING btree("assignee_id");

-- changeset moonseongha:1775294317245-79 splitStatements:false
CREATE INDEX "ix_engineering_changes_state" ON "engineering_changes" USING btree("state");

-- changeset moonseongha:1775294317245-80 splitStatements:false
CREATE INDEX "ix_engineering_changes_created_by" ON "engineering_changes" USING btree("created_by");

-- changeset moonseongha:1775294317245-81 splitStatements:false
ALTER TABLE "engineering_changes" ADD CONSTRAINT "uq_engineering_changes_number" UNIQUE ("number");

-- changeset moonseongha:1775294317245-82 splitStatements:false
CREATE INDEX "ix_files_owner_type_owner_id" ON "files" USING btree("owner_type", "owner_id");

-- changeset moonseongha:1775294317245-83 splitStatements:false
CREATE INDEX "ix_files_original_name_file_size_content_hash" ON "files" USING btree("original_name", "file_size", "content_hash");

-- changeset moonseongha:1775294317245-84 splitStatements:false
ALTER TABLE "files" ADD CONSTRAINT "uq_files_file_key" UNIQUE ("file_key");

-- changeset moonseongha:1775294317245-85 splitStatements:false
CREATE INDEX "ix_activities_target" ON "histories" USING btree("target_type", "target_id");

-- changeset moonseongha:1775294317245-86 splitStatements:false
CREATE INDEX "ix_issue_assignees_issue_id" ON "issue_assignees" USING btree("issue_id");

-- changeset moonseongha:1775294317245-87 splitStatements:false
CREATE INDEX "ix_issue_assignees_user_id" ON "issue_assignees" USING btree("user_id");

-- changeset moonseongha:1775294317245-88 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "uq_issue_assignees_issue_id_user_id" UNIQUE ("issue_id", "user_id");

-- changeset moonseongha:1775294317245-89 splitStatements:false
CREATE INDEX "ix_issue_comments_issue_id" ON "issue_comments" USING btree("issue_id");

-- changeset moonseongha:1775294317245-90 splitStatements:false
CREATE INDEX "ix_issue_labels_issue_id" ON "issue_labels" USING btree("issue_id");

-- changeset moonseongha:1775294317245-91 splitStatements:false
CREATE INDEX "ix_issue_labels_label_id" ON "issue_labels" USING btree("label_id");

-- changeset moonseongha:1775294317245-92 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "uq_issue_labels_issue_id_label_id" UNIQUE ("issue_id", "label_id");

-- changeset moonseongha:1775294317245-93 splitStatements:false
CREATE INDEX "ix_issue_parts_issue_id" ON "issue_parts" USING btree("issue_id");

-- changeset moonseongha:1775294317245-94 splitStatements:false
CREATE INDEX "ix_issue_parts_part_id" ON "issue_parts" USING btree("part_id");

-- changeset moonseongha:1775294317245-95 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "uq_issue_parts_issue_id_part_id" UNIQUE ("issue_id", "part_id");

-- changeset moonseongha:1775294317245-96 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_issue_id" ON "issue_team_assignees" USING btree("issue_id");

-- changeset moonseongha:1775294317245-97 splitStatements:false
CREATE INDEX "ix_issue_team_assignees_team_id" ON "issue_team_assignees" USING btree("team_id");

-- changeset moonseongha:1775294317245-98 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "uq_issue_team_assignees_issue_id_team_id" UNIQUE ("issue_id", "team_id");

-- changeset moonseongha:1775294317245-99 splitStatements:false
CREATE INDEX "ix_issues_state" ON "issues" USING btree("state");

-- changeset moonseongha:1775294317245-100 splitStatements:false
CREATE INDEX "ix_issues_created_by" ON "issues" USING btree("created_by");

-- changeset moonseongha:1775294317245-101 splitStatements:false
ALTER TABLE "issues" ADD CONSTRAINT "uq_issues_number" UNIQUE ("number");

-- changeset moonseongha:1775294317245-102 splitStatements:false
CREATE INDEX "ix_mapping_records_is_active" ON "mapping_records" USING btree("is_active");

-- changeset moonseongha:1775294317245-103 splitStatements:false
ALTER TABLE "mapping_records" ADD CONSTRAINT "uq_mapping_records_name" UNIQUE ("name");

-- changeset moonseongha:1775294317245-104 splitStatements:false
CREATE INDEX "ix_mapping_revisions_record_id" ON "mapping_revisions" USING btree("record_id");

-- changeset moonseongha:1775294317245-105 splitStatements:false
CREATE INDEX "ix_mapping_revisions_file_id" ON "mapping_revisions" USING btree("file_id");

-- changeset moonseongha:1775294317245-106 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "uq_mapping_revisions_record_version" UNIQUE ("record_id", "version");

-- changeset moonseongha:1775294317245-107 splitStatements:false
CREATE INDEX "ix_notifications_user_unread" ON "notifications" USING btree("user_id", "read_at");

-- changeset moonseongha:1775294317245-108 splitStatements:false
CREATE INDEX "ix_part_revision_histories_revision_created" ON "part_revision_histories" USING btree("part_revision_id", "created_at");

-- changeset moonseongha:1775294317245-109 splitStatements:false
CREATE INDEX "ix_part_revision_histories_actor_id" ON "part_revision_histories" USING btree("actor_id");

-- changeset moonseongha:1775294317245-110 splitStatements:false
CREATE INDEX "ix_part_revision_histories_source_ref_id" ON "part_revision_histories" USING btree("source_ref_id");

-- changeset moonseongha:1775294317245-111 splitStatements:false
CREATE INDEX "ix_part_revisions_part_id" ON "part_revisions" USING btree("part_id");

-- changeset moonseongha:1775294317245-112 splitStatements:false
CREATE INDEX "ix_part_revisions_part_number" ON "part_revisions" USING btree("part_number");

-- changeset moonseongha:1775294317245-113 splitStatements:false
CREATE INDEX "ix_part_revisions_base_revision_id" ON "part_revisions" USING btree("base_revision_id");

-- changeset moonseongha:1775294317245-114 splitStatements:false
CREATE INDEX "ix_part_revisions_created_by" ON "part_revisions" USING btree("created_by");

-- changeset moonseongha:1775294317245-115 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "uq_part_revisions_part_number_revision_code" UNIQUE ("part_number", "revision_code");

-- changeset moonseongha:1775294317245-116 splitStatements:false
CREATE INDEX "ix_part_suppliers_part_revision_id" ON "part_suppliers" USING btree("part_revision_id");

-- changeset moonseongha:1775294317245-117 splitStatements:false
CREATE INDEX "ix_part_suppliers_supplier_id" ON "part_suppliers" USING btree("supplier_id");

-- changeset moonseongha:1775294317245-118 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "uq_part_suppliers_part_revision_id_supplier_id" UNIQUE ("part_revision_id", "supplier_id");

-- changeset moonseongha:1775294317245-119 splitStatements:false
CREATE INDEX "ix_project_members_project_id" ON "project_members" USING btree("project_id");

-- changeset moonseongha:1775294317245-120 splitStatements:false
CREATE INDEX "ix_project_members_user_id" ON "project_members" USING btree("user_id");

-- changeset moonseongha:1775294317245-121 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "uq_project_members_project_id_user_id" UNIQUE ("project_id", "user_id");

-- changeset moonseongha:1775294317245-122 splitStatements:false
CREATE INDEX "ix_project_parts_project_id" ON "project_parts" USING btree("project_id");

-- changeset moonseongha:1775294317245-123 splitStatements:false
CREATE INDEX "ix_project_parts_part_id" ON "project_parts" USING btree("part_id");

-- changeset moonseongha:1775294317245-124 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "uq_project_parts_project_id_part_id" UNIQUE ("project_id", "part_id");

-- changeset moonseongha:1775294317245-125 splitStatements:false
CREATE INDEX "ix_property_definitions_owner_type_is_active_display_order" ON "property_definitions" USING btree("owner_type", "is_active", "display_order");

-- changeset moonseongha:1775294317245-126 splitStatements:false
ALTER TABLE "property_definitions" ADD CONSTRAINT "uq_property_definitions_owner_type_property_key" UNIQUE ("owner_type", "property_key");

-- changeset moonseongha:1775294317245-127 splitStatements:false
CREATE INDEX "ix_suppliers_code" ON "suppliers" USING btree("code");

-- changeset moonseongha:1775294317245-128 splitStatements:false
ALTER TABLE "suppliers" ADD CONSTRAINT "uq_suppliers_company_name" UNIQUE ("company_name");

-- changeset moonseongha:1775294317245-129 splitStatements:false
CREATE INDEX "ix_synthesis_batches_project_id" ON "synthesis_batches" USING btree("project_id");

-- changeset moonseongha:1775294317245-130 splitStatements:false
CREATE INDEX "ix_synthesis_batches_mapping_id" ON "synthesis_batches" USING btree("mapping_id");

-- changeset moonseongha:1775294317245-131 splitStatements:false
CREATE INDEX "ix_synthesis_batches_requested_by" ON "synthesis_batches" USING btree("requested_by");

-- changeset moonseongha:1775294317245-132 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_batch_id" ON "synthesis_jobs" USING btree("batch_id");

-- changeset moonseongha:1775294317245-133 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_mapping_id" ON "synthesis_jobs" USING btree("mapping_id");

-- changeset moonseongha:1775294317245-134 splitStatements:false
CREATE INDEX "ix_synthesis_jobs_file_id" ON "synthesis_jobs" USING btree("file_id");

-- changeset moonseongha:1775294317245-135 splitStatements:false
CREATE INDEX "ix_team_members_team_id" ON "team_members" USING btree("team_id");

-- changeset moonseongha:1775294317245-136 splitStatements:false
CREATE INDEX "ix_team_members_user_id" ON "team_members" USING btree("user_id");

-- changeset moonseongha:1775294317245-137 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "uq_team_members_team_id_user_id" UNIQUE ("team_id", "user_id");

-- changeset moonseongha:1775294317245-138 splitStatements:false
ALTER TABLE "drawing_artifacts" ADD CONSTRAINT "uq_drawing_artifacts_drawing_type" UNIQUE ("drawing_id", "artifact_type");

-- changeset moonseongha:1775294317245-139 splitStatements:false
ALTER TABLE "part_preview_artifacts" ADD CONSTRAINT "uq_part_preview_artifacts_preview_type" UNIQUE ("part_preview_id", "artifact_type");

-- changeset moonseongha:1775294317245-140 splitStatements:false
ALTER TABLE "part_previews" ADD CONSTRAINT "uq_part_previews_part_revision_id" UNIQUE ("part_revision_id");

-- changeset moonseongha:1775294317245-141 splitStatements:false
ALTER TABLE "part_revision_workflow_policies" ADD CONSTRAINT "uq_part_revision_workflow_policies_policy_key" UNIQUE ("policy_key");

-- changeset moonseongha:1775294317245-142 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "uq_parts_part_number" UNIQUE ("part_number");

-- changeset moonseongha:1775294317245-143 splitStatements:false
ALTER TABLE "labels" ADD CONSTRAINT "uq_labels_name" UNIQUE ("name");

-- changeset moonseongha:1775294317245-144 splitStatements:false
ALTER TABLE "part_number_categories" ADD CONSTRAINT "uq_part_number_categories_name" UNIQUE ("name");

-- changeset moonseongha:1775294317245-145 splitStatements:false
ALTER TABLE "part_number_categories" ADD CONSTRAINT "uq_part_number_categories_prefix" UNIQUE ("prefix");

-- changeset moonseongha:1775294317245-146 splitStatements:false
ALTER TABLE "part_number_sequences" ADD CONSTRAINT "uq_part_number_sequences_category_id" UNIQUE ("category_id");

-- changeset moonseongha:1775294317245-147 splitStatements:false
ALTER TABLE "part_preview_files" ADD CONSTRAINT "uq_part_preview_files_file_id" UNIQUE ("file_id");

-- changeset moonseongha:1775294317245-148 splitStatements:false
ALTER TABLE "teams" ADD CONSTRAINT "uq_teams_name" UNIQUE ("name");

-- changeset moonseongha:1775294317245-149 splitStatements:false
ALTER TABLE "engineering_change_comments" ADD CONSTRAINT "fk1g95iugo186w07u6gawmanhdr" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-150 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk2w9t9pm4a18rihij6xcup7kun" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-151 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fk4brv9b343p36yict83fyfixdy" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-152 splitStatements:false
ALTER TABLE "issue_team_assignees" ADD CONSTRAINT "fk56bv9knlohikvliauqx0s6x35" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-153 splitStatements:false
ALTER TABLE "project_parts" ADD CONSTRAINT "fk5ddsht7he9hab9xu47wnook3f" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-154 splitStatements:false
ALTER TABLE "engineering_change_issues" ADD CONSTRAINT "fk5i2rg8agkl18atn0jbjaxr8ol" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-155 splitStatements:false
ALTER TABLE "issue_assignees" ADD CONSTRAINT "fk81q8jhewj6k8k5s1y4cw1wdyo" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-156 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk_mapping_revisions_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-157 splitStatements:false
ALTER TABLE "mapping_revisions" ADD CONSTRAINT "fk_mapping_revisions_record_id" FOREIGN KEY ("record_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-158 splitStatements:false
ALTER TABLE "part_revision_histories" ADD CONSTRAINT "fk_part_revision_histories_part_revision_id" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-159 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-160 splitStatements:false
ALTER TABLE "synthesis_batches" ADD CONSTRAINT "fk_synthesis_batches_project_id" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-161 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_file_id" FOREIGN KEY ("file_id") REFERENCES "files" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-162 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fk_synthesis_jobs_mapping_id" FOREIGN KEY ("mapping_id") REFERENCES "mapping_records" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-163 splitStatements:false
ALTER TABLE "team_members" ADD CONSTRAINT "fk_team_members_team_id" FOREIGN KEY ("team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-164 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "fkayul0e21mxb13ncdi8kae6obr" FOREIGN KEY ("child_part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-165 splitStatements:false
ALTER TABLE "part_previews" ADD CONSTRAINT "fkbvtyw5qsiaevxfqwdonbc242x" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-166 splitStatements:false
ALTER TABLE "drawing_artifacts" ADD CONSTRAINT "fkcvt2rdwm199odunwkj17i1k8n" FOREIGN KEY ("drawing_id") REFERENCES "drawings" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-167 splitStatements:false
ALTER TABLE "part_suppliers" ADD CONSTRAINT "fkd47ukwxcpdb87h3b6kd017k9n" FOREIGN KEY ("supplier_id") REFERENCES "suppliers" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-168 splitStatements:false
ALTER TABLE "project_members" ADD CONSTRAINT "fkdki1sp2homqsdcvqm9yrix31g" FOREIGN KEY ("project_id") REFERENCES "projects" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-169 splitStatements:false
ALTER TABLE "issue_parts" ADD CONSTRAINT "fkeee47ncsdhj0917jfcjnrbrvv" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-170 splitStatements:false
ALTER TABLE "engineering_bom_items" ADD CONSTRAINT "fkelfms0mcqge2jlvp4podgjunj" FOREIGN KEY ("parent_part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-171 splitStatements:false
ALTER TABLE "synthesis_jobs" ADD CONSTRAINT "fkf4vylyr6m7ofb2jd28fla6ry7" FOREIGN KEY ("batch_id") REFERENCES "synthesis_batches" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-172 splitStatements:false
ALTER TABLE "issue_labels" ADD CONSTRAINT "fkfskpaxuixega1rlf9hrhnhngs" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-173 splitStatements:false
ALTER TABLE "part_preview_files" ADD CONSTRAINT "fkhovn2awghkyvs82v0jblam02p" FOREIGN KEY ("part_preview_id") REFERENCES "part_previews" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-174 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fki6fs3anwh4t24809xn23xilu4" FOREIGN KEY ("base_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-175 splitStatements:false
ALTER TABLE "part_revisions" ADD CONSTRAINT "fkl1cfng9egr5y8m0chwm32y9di" FOREIGN KEY ("part_id") REFERENCES "parts" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-176 splitStatements:false
ALTER TABLE "part_preview_artifacts" ADD CONSTRAINT "fklauu1wqvbrynnfd8v9r0hquj9" FOREIGN KEY ("part_preview_id") REFERENCES "part_previews" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-177 splitStatements:false
ALTER TABLE "engineering_change_steps" ADD CONSTRAINT "fkm9g6yiohm3rlx0qv3gr1y0040" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-178 splitStatements:false
ALTER TABLE "engineering_change_affected_items" ADD CONSTRAINT "fknkmd2kximv3deux7l6jx0e9el" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-179 splitStatements:false
ALTER TABLE "issue_comments" ADD CONSTRAINT "fknvnj0204928o0w1th5jsx4f28" FOREIGN KEY ("issue_id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-180 splitStatements:false
ALTER TABLE "drawings" ADD CONSTRAINT "fkovfe3bykl1mphod4f8odh26oc" FOREIGN KEY ("part_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775294317245-181 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "fkqewjubof3sw38w93c20x4j4xf" FOREIGN KEY ("current_released_revision_id") REFERENCES "part_revisions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

