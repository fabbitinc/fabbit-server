-- Create "ai_usage_logs" table
CREATE TABLE "ai_usage_logs" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "category" character varying(30) NOT NULL,
  "credits_used" numeric(10,4) NOT NULL,
  "feature" character varying(50) NOT NULL,
  "input_tokens" integer NOT NULL,
  "model" character varying(50) NOT NULL,
  "org_id" uuid NOT NULL,
  "output_tokens" integer NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id")
);
-- Create index "ix_ai_usage_logs_org_id" to table: "ai_usage_logs"
CREATE INDEX "ix_ai_usage_logs_org_id" ON "ai_usage_logs" ("org_id");
-- Create index "ix_ai_usage_logs_org_id_created_at" to table: "ai_usage_logs"
CREATE INDEX "ix_ai_usage_logs_org_id_created_at" ON "ai_usage_logs" ("org_id", "created_at");
-- Create index "ix_ai_usage_logs_user_id" to table: "ai_usage_logs"
CREATE INDEX "ix_ai_usage_logs_user_id" ON "ai_usage_logs" ("user_id");
-- Create "email_verifications" table
CREATE TABLE "email_verifications" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "attempt_count" integer NOT NULL,
  "code_hash" character varying(64) NOT NULL,
  "email" character varying(255) NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "status" character varying(20) NOT NULL,
  "verification_token_hash" character varying(64) NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "email_verifications_status_check" CHECK ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'VERIFIED'::character varying, 'USED'::character varying])::text[]))
);
-- Create index "ix_email_verifications_email" to table: "email_verifications"
CREATE INDEX "ix_email_verifications_email" ON "email_verifications" ("email");
-- Create "invitations" table
CREATE TABLE "invitations" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "accepted_at" timestamptz NULL,
  "email" character varying(255) NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "invited_by" uuid NOT NULL,
  "org_id" uuid NOT NULL,
  "role" character varying(20) NOT NULL,
  "status" character varying(20) NOT NULL,
  "token_hash" character varying(64) NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_invitations_org_id_email" UNIQUE ("org_id", "email"),
  CONSTRAINT "uq_invitations_token_hash" UNIQUE ("token_hash"),
  CONSTRAINT "invitations_role_check" CHECK ((role)::text = ANY ((ARRAY['MEMBER'::character varying, 'ADMIN'::character varying, 'OWNER'::character varying])::text[])),
  CONSTRAINT "invitations_status_check" CHECK ((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'CANCELLED'::character varying])::text[]))
);
-- Create index "ix_invitations_invited_by" to table: "invitations"
CREATE INDEX "ix_invitations_invited_by" ON "invitations" ("invited_by");
-- Create index "ix_invitations_org_id" to table: "invitations"
CREATE INDEX "ix_invitations_org_id" ON "invitations" ("org_id");
-- Create "memberships" table
CREATE TABLE "memberships" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "job_role" character varying(50) NULL,
  "org_id" uuid NOT NULL,
  "role" character varying(20) NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_memberships_user_id_org_id" UNIQUE ("user_id", "org_id"),
  CONSTRAINT "memberships_role_check" CHECK ((role)::text = ANY ((ARRAY['MEMBER'::character varying, 'ADMIN'::character varying, 'OWNER'::character varying])::text[]))
);
-- Create index "ix_memberships_org_id" to table: "memberships"
CREATE INDEX "ix_memberships_org_id" ON "memberships" ("org_id");
-- Create "organizations" table
CREATE TABLE "organizations" (
  "id" uuid NOT NULL,
  "allow_storage_overage" boolean NOT NULL,
  "bonus_credits_remaining" integer NOT NULL,
  "industry" character varying(50) NULL,
  "max_members" integer NOT NULL,
  "name" character varying(100) NOT NULL,
  "owner_id" uuid NOT NULL,
  "plan_credits_remaining" integer NOT NULL,
  "plan_type" character varying(20) NOT NULL,
  "profile_image_file_key" character varying(255) NULL,
  "slug" character varying(50) NOT NULL,
  "storage_bytes_limit" bigint NOT NULL,
  "storage_bytes_used" bigint NOT NULL,
  "team_size" character varying(20) NULL,
  "used_members" integer NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "organizations_slug_key" UNIQUE ("slug"),
  CONSTRAINT "organizations_plan_type_check" CHECK ((plan_type)::text = ANY ((ARRAY['STARTER'::character varying, 'TEAM'::character varying, 'BUSINESS'::character varying, 'ENTERPRISE'::character varying])::text[]))
);
-- Create index "ix_organizations_owner_id" to table: "organizations"
CREATE INDEX "ix_organizations_owner_id" ON "organizations" ("owner_id");
-- Create "refresh_tokens" table
CREATE TABLE "refresh_tokens" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "expires_at" timestamptz NOT NULL,
  "token_jti" character varying(36) NOT NULL,
  "user_id" uuid NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_refresh_tokens_token_jti" UNIQUE ("token_jti")
);
-- Create index "ix_refresh_tokens_user_id" to table: "refresh_tokens"
CREATE INDEX "ix_refresh_tokens_user_id" ON "refresh_tokens" ("user_id");
-- Create "subscriptions" table
CREATE TABLE "subscriptions" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "ai_credits_granted" integer NOT NULL,
  "cancel_at_period_end" boolean NOT NULL,
  "current_period_end" timestamptz NOT NULL,
  "current_period_start" timestamptz NOT NULL,
  "max_members" integer NOT NULL,
  "org_id" uuid NOT NULL,
  "plan_type" character varying(20) NOT NULL,
  "status" character varying(20) NOT NULL,
  "storage_bytes_limit" bigint NOT NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "subscriptions_status_check" CHECK ((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PAST_DUE'::character varying, 'CANCELED'::character varying, 'EXPIRED'::character varying])::text[]))
);
-- Create index "ix_subscriptions_org_id" to table: "subscriptions"
CREATE INDEX "ix_subscriptions_org_id" ON "subscriptions" ("org_id");
-- Create "users" table
CREATE TABLE "users" (
  "id" uuid NOT NULL,
  "created_at" timestamptz NOT NULL,
  "updated_at" timestamptz NOT NULL,
  "is_active" boolean NOT NULL,
  "email" character varying(255) NOT NULL,
  "full_name" character varying(100) NOT NULL,
  "hashed_password" character varying(255) NOT NULL,
  "phone" character varying(20) NULL,
  "profile_image_file_key" character varying(1000) NULL,
  PRIMARY KEY ("id"),
  CONSTRAINT "uq_users_email" UNIQUE ("email")
);
