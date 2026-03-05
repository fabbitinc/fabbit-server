-- Create "labels" table
CREATE TABLE "labels" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "color" character varying(7) NOT NULL,
  "created_by" uuid NULL,
  "description" character varying(200) NULL,
  "name" character varying(50) NOT NULL,
  "updated_by" uuid NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_labels_name" UNIQUE ("name")
);
-- Create "part_suppliers" table
CREATE TABLE "part_suppliers" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "extended_properties" jsonb NOT NULL,
  "part_id" uuid NOT NULL,
  "supplier_id" uuid NOT NULL,
  "unit_cost" double precision NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_part_suppliers_part_id_supplier_id" UNIQUE ("part_id", "supplier_id")
);
-- Create index "ix_part_suppliers_part_id" to table: "part_suppliers"
CREATE INDEX "ix_part_suppliers_part_id" ON "part_suppliers" ("part_id");
-- Create index "ix_part_suppliers_supplier_id" to table: "part_suppliers"
CREATE INDEX "ix_part_suppliers_supplier_id" ON "part_suppliers" ("supplier_id");
-- Create "change_request_issues" table
CREATE TABLE "change_request_issues" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "change_request_id" uuid NOT NULL,
  "issue_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_change_request_issues_cr_id_issue_id" UNIQUE ("change_request_id", "issue_id")
);
-- Create index "ix_change_request_issues_change_request_id" to table: "change_request_issues"
CREATE INDEX "ix_change_request_issues_change_request_id" ON "change_request_issues" ("change_request_id");
-- Create index "ix_change_request_issues_issue_id" to table: "change_request_issues"
CREATE INDEX "ix_change_request_issues_issue_id" ON "change_request_issues" ("issue_id");
-- Create "change_request_reviewers" table
CREATE TABLE "change_request_reviewers" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "change_request_id" uuid NOT NULL,
  "review_status" character varying(20) NOT NULL,
  "reviewed_at" timestamptz NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_cr_reviewers_cr_id_user_id" UNIQUE ("change_request_id", "user_id")
);
-- Create index "ix_cr_reviewers_change_request_id" to table: "change_request_reviewers"
CREATE INDEX "ix_cr_reviewers_change_request_id" ON "change_request_reviewers" ("change_request_id");
-- Create index "ix_cr_reviewers_user_id" to table: "change_request_reviewers"
CREATE INDEX "ix_cr_reviewers_user_id" ON "change_request_reviewers" ("user_id");
-- Create "synthesis_jobs" table
CREATE TABLE "synthesis_jobs" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "batch_id" uuid NULL,
  "completed_at" timestamptz NULL,
  "errors" jsonb NOT NULL,
  "file_id" uuid NOT NULL,
  "mapping_id" uuid NOT NULL,
  "nodes_created" integer NOT NULL,
  "processed_rows" integer NOT NULL,
  "relationships_created" integer NOT NULL,
  "started_at" timestamptz NULL,
  "status" character varying(20) NOT NULL,
  "total_rows" integer NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "ix_synthesis_jobs_batch_id" to table: "synthesis_jobs"
CREATE INDEX "ix_synthesis_jobs_batch_id" ON "synthesis_jobs" ("batch_id");
-- Create index "ix_synthesis_jobs_file_id" to table: "synthesis_jobs"
CREATE INDEX "ix_synthesis_jobs_file_id" ON "synthesis_jobs" ("file_id");
-- Create index "ix_synthesis_jobs_mapping_id" to table: "synthesis_jobs"
CREATE INDEX "ix_synthesis_jobs_mapping_id" ON "synthesis_jobs" ("mapping_id");
-- Create "cr_team_reviewers" table
CREATE TABLE "cr_team_reviewers" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "change_request_id" uuid NOT NULL,
  "team_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_cr_team_reviewers_cr_id_team_id" UNIQUE ("change_request_id", "team_id")
);
-- Create index "ix_cr_team_reviewers_change_request_id" to table: "cr_team_reviewers"
CREATE INDEX "ix_cr_team_reviewers_change_request_id" ON "cr_team_reviewers" ("change_request_id");
-- Create index "ix_cr_team_reviewers_team_id" to table: "cr_team_reviewers"
CREATE INDEX "ix_cr_team_reviewers_team_id" ON "cr_team_reviewers" ("team_id");
-- Create "drawings" table
CREATE TABLE "drawings" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "conversion_status" character varying(30) NULL,
  "deleted_at" timestamptz NULL,
  "drawing_number" character varying(100) NULL,
  "name" character varying(500) NULL,
  "original_file_key" character varying(1000) NULL,
  "pdf_key" character varying(1000) NULL,
  "status" character varying(50) NULL,
  "thumbnail_key" character varying(1000) NULL,
  "version" character varying(50) NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_drawings_drawing_number" UNIQUE ("drawing_number")
);
-- Create "files" table
CREATE TABLE "files" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "content_type" character varying(100) NOT NULL,
  "deleted_at" timestamptz NULL,
  "file_key" character varying(1000) NOT NULL,
  "file_size" bigint NOT NULL,
  "original_name" character varying(500) NOT NULL,
  "owner_id" uuid NULL,
  "owner_type" character varying(50) NULL,
  "status" character varying(20) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_files_file_key" UNIQUE ("file_key"),
  CONSTRAINT "files_status_check" CHECK ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'UPLOADED'::character varying])::text[]))
);
-- Create index "ix_files_owner_type_owner_id" to table: "files"
CREATE INDEX "ix_files_owner_type_owner_id" ON "files" ("owner_type", "owner_id");
-- Create "issue_assignees" table
CREATE TABLE "issue_assignees" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "issue_id" uuid NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_issue_assignees_issue_id_user_id" UNIQUE ("issue_id", "user_id")
);
-- Create index "ix_issue_assignees_issue_id" to table: "issue_assignees"
CREATE INDEX "ix_issue_assignees_issue_id" ON "issue_assignees" ("issue_id");
-- Create index "ix_issue_assignees_user_id" to table: "issue_assignees"
CREATE INDEX "ix_issue_assignees_user_id" ON "issue_assignees" ("user_id");
-- Create "issue_comments" table
CREATE TABLE "issue_comments" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "body" text NOT NULL,
  "created_by" uuid NOT NULL,
  "issue_id" uuid NOT NULL,
  "updated_by" uuid NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "ix_issue_comments_issue_id" to table: "issue_comments"
CREATE INDEX "ix_issue_comments_issue_id" ON "issue_comments" ("issue_id");
-- Create "issue_labels" table
CREATE TABLE "issue_labels" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "issue_id" uuid NOT NULL,
  "label_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_issue_labels_issue_id_label_id" UNIQUE ("issue_id", "label_id")
);
-- Create index "ix_issue_labels_issue_id" to table: "issue_labels"
CREATE INDEX "ix_issue_labels_issue_id" ON "issue_labels" ("issue_id");
-- Create index "ix_issue_labels_label_id" to table: "issue_labels"
CREATE INDEX "ix_issue_labels_label_id" ON "issue_labels" ("label_id");
-- Create "issue_parts" table
CREATE TABLE "issue_parts" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "issue_id" uuid NOT NULL,
  "part_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_issue_parts_issue_id_part_id" UNIQUE ("issue_id", "part_id")
);
-- Create index "ix_issue_parts_issue_id" to table: "issue_parts"
CREATE INDEX "ix_issue_parts_issue_id" ON "issue_parts" ("issue_id");
-- Create index "ix_issue_parts_part_id" to table: "issue_parts"
CREATE INDEX "ix_issue_parts_part_id" ON "issue_parts" ("part_id");
-- Create "issue_team_assignees" table
CREATE TABLE "issue_team_assignees" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "issue_id" uuid NOT NULL,
  "team_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_issue_team_assignees_issue_id_team_id" UNIQUE ("issue_id", "team_id")
);
-- Create index "ix_issue_team_assignees_issue_id" to table: "issue_team_assignees"
CREATE INDEX "ix_issue_team_assignees_issue_id" ON "issue_team_assignees" ("issue_id");
-- Create index "ix_issue_team_assignees_team_id" to table: "issue_team_assignees"
CREATE INDEX "ix_issue_team_assignees_team_id" ON "issue_team_assignees" ("team_id");
-- Create "issues" table
CREATE TABLE "issues" (
  "type" character varying(20) NOT NULL,
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "body" text NULL,
  "closed_at" timestamptz NULL,
  "created_by" uuid NOT NULL,
  "number" integer NOT NULL,
  "state" character varying(20) NOT NULL,
  "title" character varying(500) NOT NULL,
  "updated_by" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_issues_number" UNIQUE ("number"),
  CONSTRAINT "issues_state_check" CHECK ((state)::text = ANY ((ARRAY['OPEN'::character varying, 'CLOSED'::character varying])::text[])),
  CONSTRAINT "issues_type_check" CHECK ((type)::text = ANY ((ARRAY['ISSUE'::character varying, 'CHANGE_REQUEST'::character varying])::text[]))
);
-- Create index "ix_issues_created_by" to table: "issues"
CREATE INDEX "ix_issues_created_by" ON "issues" ("created_by");
-- Create index "ix_issues_type_state" to table: "issues"
CREATE INDEX "ix_issues_type_state" ON "issues" ("type", "state");
-- Create "bom_links" table
CREATE TABLE "bom_links" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "child_part_id" uuid NOT NULL,
  "extended_properties" jsonb NOT NULL,
  "parent_part_id" uuid NOT NULL,
  "quantity" integer NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_bom_links_parent_part_id_child_part_id" UNIQUE ("parent_part_id", "child_part_id")
);
-- Create index "ix_bom_links_child_part_id" to table: "bom_links"
CREATE INDEX "ix_bom_links_child_part_id" ON "bom_links" ("child_part_id");
-- Create index "ix_bom_links_parent_part_id" to table: "bom_links"
CREATE INDEX "ix_bom_links_parent_part_id" ON "bom_links" ("parent_part_id");
-- Create "mapping_revisions" table
CREATE TABLE "mapping_revisions" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "file_id" uuid NOT NULL,
  "mapping" jsonb NOT NULL,
  "original_headers" jsonb NOT NULL,
  "record_id" uuid NOT NULL,
  "sheet_name" character varying(200) NULL,
  "usage_count" integer NOT NULL,
  "version" integer NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_mapping_revisions_record_version" UNIQUE ("record_id", "version")
);
-- Create index "ix_mapping_revisions_file_id" to table: "mapping_revisions"
CREATE INDEX "ix_mapping_revisions_file_id" ON "mapping_revisions" ("file_id");
-- Create index "ix_mapping_revisions_record_id" to table: "mapping_revisions"
CREATE INDEX "ix_mapping_revisions_record_id" ON "mapping_revisions" ("record_id");
-- Create "activities" table
CREATE TABLE "activities" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "action" character varying(50) NOT NULL,
  "actor_id" uuid NOT NULL,
  "detail" jsonb NULL,
  "target_id" uuid NOT NULL,
  "target_type" character varying(20) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "activities_target_type_check" CHECK ((target_type)::text = ANY ((ARRAY['PROJECT'::character varying, 'ISSUE'::character varying, 'ORGANIZATION'::character varying])::text[]))
);
-- Create index "ix_activities_target" to table: "activities"
CREATE INDEX "ix_activities_target" ON "activities" ("target_type", "target_id");
-- Create "notifications" table
CREATE TABLE "notifications" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "actor_id" uuid NOT NULL,
  "payload" jsonb NOT NULL,
  "read_at" timestamptz NULL,
  "type" character varying(20) NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "notifications_type_check" CHECK ((type)::text = 'MENTION'::text)
);
-- Create index "ix_notifications_user_unread" to table: "notifications"
CREATE INDEX "ix_notifications_user_unread" ON "notifications" ("user_id", "read_at");
-- Create "part_default_owners" table
CREATE TABLE "part_default_owners" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "category" character varying(100) NULL,
  "default_owner_id" uuid NULL,
  "default_owner_team_id" uuid NULL,
  PRIMARY KEY ("id")
);
-- Create "mapping_records" table
CREATE TABLE "mapping_records" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "is_active" boolean NOT NULL,
  "name" character varying(200) NOT NULL,
  "scope" character varying(20) NOT NULL,
  "usage_count" integer NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_mapping_records_name" UNIQUE ("name")
);
-- Create index "ix_mapping_records_scope_is_active" to table: "mapping_records"
CREATE INDEX "ix_mapping_records_scope_is_active" ON "mapping_records" ("scope", "is_active");
-- Create "parts" table
CREATE TABLE "parts" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "category" character varying(100) NULL,
  "description" text NULL,
  "drawing_id" uuid NULL,
  "extended_properties" jsonb NOT NULL,
  "lead_time_days" integer NULL,
  "lifecycle_state" character varying(50) NULL,
  "material" character varying(200) NULL,
  "name" character varying(500) NULL,
  "owner_id" uuid NULL,
  "owner_team_id" uuid NULL,
  "part_number" character varying(100) NOT NULL,
  "is_phantom" boolean NULL,
  "revision" character varying(50) NOT NULL,
  "unit" character varying(20) NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_parts_part_number" UNIQUE ("part_number")
);
-- Create "project_members" table
CREATE TABLE "project_members" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "project_id" uuid NOT NULL,
  "role" character varying(20) NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_project_members_project_id_user_id" UNIQUE ("project_id", "user_id"),
  CONSTRAINT "project_members_role_check" CHECK ((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'MEMBER'::character varying, 'VIEWER'::character varying])::text[]))
);
-- Create index "ix_project_members_project_id" to table: "project_members"
CREATE INDEX "ix_project_members_project_id" ON "project_members" ("project_id");
-- Create index "ix_project_members_user_id" to table: "project_members"
CREATE INDEX "ix_project_members_user_id" ON "project_members" ("user_id");
-- Create "project_parts" table
CREATE TABLE "project_parts" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "part_id" uuid NOT NULL,
  "project_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_project_parts_project_id_part_id" UNIQUE ("project_id", "part_id")
);
-- Create index "ix_project_parts_part_id" to table: "project_parts"
CREATE INDEX "ix_project_parts_part_id" ON "project_parts" ("part_id");
-- Create index "ix_project_parts_project_id" to table: "project_parts"
CREATE INDEX "ix_project_parts_project_id" ON "project_parts" ("project_id");
-- Create "projects" table
CREATE TABLE "projects" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "created_by" character varying(100) NULL,
  "updated_by" character varying(100) NULL,
  "is_deleted" boolean NOT NULL,
  "deleted_at" timestamptz NULL,
  "deleted_by" character varying(100) NULL,
  "is_archived" boolean NOT NULL,
  "description" text NULL,
  "name" character varying(200) NOT NULL,
  PRIMARY KEY ("id")
);
-- Create "suppliers" table
CREATE TABLE "suppliers" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "code" character varying(100) NULL,
  "company_name" character varying(200) NOT NULL,
  "contact_info" text NULL,
  "country" character varying(100) NULL,
  "extended_properties" jsonb NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_suppliers_company_name" UNIQUE ("company_name")
);
-- Create index "ix_suppliers_code" to table: "suppliers"
CREATE INDEX "ix_suppliers_code" ON "suppliers" ("code");
-- Create "synthesis_batches" table
CREATE TABLE "synthesis_batches" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "accepted_count" integer NOT NULL,
  "failed_uploads" jsonb NOT NULL,
  "mapping_id" uuid NOT NULL,
  "project_id" uuid NULL,
  "requested_count" integer NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "ix_synthesis_batches_mapping_id" to table: "synthesis_batches"
CREATE INDEX "ix_synthesis_batches_mapping_id" ON "synthesis_batches" ("mapping_id");
-- Create index "ix_synthesis_batches_project_id" to table: "synthesis_batches"
CREATE INDEX "ix_synthesis_batches_project_id" ON "synthesis_batches" ("project_id");
-- Create "change_requests" table
CREATE TABLE "change_requests" (
  "cr_state" character varying(20) NOT NULL,
  "merged_at" timestamptz NULL,
  "merged_by" uuid NULL,
  "id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "fklbxmcabro1hxy8yx414vtb86y" FOREIGN KEY ("id") REFERENCES "issues" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
  CONSTRAINT "change_requests_cr_state_check" CHECK ((cr_state)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'MERGED'::character varying, 'CLOSED'::character varying])::text[]))
);
-- Create "teams" table
CREATE TABLE "teams" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "created_by" uuid NOT NULL,
  "description" text NULL,
  "name" character varying(100) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_teams_name" UNIQUE ("name")
);
-- Create "team_members" table
CREATE TABLE "team_members" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "user_id" uuid NOT NULL,
  "team_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_team_members_team_id_user_id" UNIQUE ("team_id", "user_id"),
  CONSTRAINT "fk_team_members_team_id" FOREIGN KEY ("team_id") REFERENCES "teams" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);
-- Create index "ix_team_members_team_id" to table: "team_members"
CREATE INDEX "ix_team_members_team_id" ON "team_members" ("team_id");
-- Create index "ix_team_members_user_id" to table: "team_members"
CREATE INDEX "ix_team_members_user_id" ON "team_members" ("user_id");
