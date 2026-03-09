-- liquibase formatted sql

-- changeset seongha.moon:1773067471545-1 splitStatements:false
CREATE TABLE "ai_usage_logs" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "category" VARCHAR(30) NOT NULL, "credits_used" numeric(10, 4) NOT NULL, "feature" VARCHAR(50) NOT NULL, "input_tokens" INTEGER NOT NULL, "model" VARCHAR(50) NOT NULL, "org_id" UUID NOT NULL, "output_tokens" INTEGER NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "ai_usage_logs_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-2 splitStatements:false
CREATE TABLE "email_verifications" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "attempt_count" INTEGER NOT NULL, "code_hash" VARCHAR(64) NOT NULL, "email" VARCHAR(255) NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "status" VARCHAR(20) NOT NULL, "verification_token_hash" VARCHAR(64), CONSTRAINT "email_verifications_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-3 splitStatements:false
CREATE TABLE "invitations" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "accepted_at" TIMESTAMP WITH TIME ZONE, "email" VARCHAR(255) NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "invited_by" UUID NOT NULL, "org_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "status" VARCHAR(20) NOT NULL, "token_hash" VARCHAR(64) NOT NULL, CONSTRAINT "invitations_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-4 splitStatements:false
CREATE TABLE "memberships" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "job_role" VARCHAR(50), "org_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "memberships_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-5 splitStatements:false
CREATE TABLE "organizations" ("id" UUID NOT NULL, "allow_storage_overage" BOOLEAN NOT NULL, "bonus_credits_remaining" INTEGER NOT NULL, "industry" VARCHAR(50), "max_members" INTEGER NOT NULL, "name" VARCHAR(100) NOT NULL, "owner_id" UUID NOT NULL, "plan_credits_remaining" INTEGER NOT NULL, "plan_type" VARCHAR(20) NOT NULL, "profile_image_file_key" VARCHAR(255), "slug" VARCHAR(50) NOT NULL, "storage_bytes_limit" BIGINT NOT NULL, "storage_bytes_used" BIGINT NOT NULL, "team_size" VARCHAR(20), "used_members" INTEGER NOT NULL, CONSTRAINT "organizations_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-6 splitStatements:false
CREATE TABLE "refresh_tokens" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "revoked_at" TIMESTAMP WITH TIME ZONE, "token_jti" VARCHAR(36) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-7 splitStatements:false
CREATE TABLE "subscriptions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "ai_credits_granted" INTEGER NOT NULL, "cancel_at_period_end" BOOLEAN NOT NULL, "current_period_end" TIMESTAMP WITH TIME ZONE NOT NULL, "current_period_start" TIMESTAMP WITH TIME ZONE NOT NULL, "max_members" INTEGER NOT NULL, "org_id" UUID NOT NULL, "plan_type" VARCHAR(20) NOT NULL, "status" VARCHAR(20) NOT NULL, "storage_bytes_limit" BIGINT NOT NULL, CONSTRAINT "subscriptions_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-8 splitStatements:false
CREATE TABLE "users" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "email" VARCHAR(255) NOT NULL, "full_name" VARCHAR(100) NOT NULL, "hashed_password" VARCHAR(255) NOT NULL, "phone" VARCHAR(20), "profile_image_file_key" VARCHAR(1000), CONSTRAINT "users_pkey" PRIMARY KEY ("id"));

-- changeset seongha.moon:1773067471545-9 splitStatements:false
CREATE INDEX "ix_ai_usage_logs_org_id" ON "ai_usage_logs" USING btree("org_id");

-- changeset seongha.moon:1773067471545-10 splitStatements:false
CREATE INDEX "ix_ai_usage_logs_user_id" ON "ai_usage_logs" USING btree("user_id");

-- changeset seongha.moon:1773067471545-11 splitStatements:false
CREATE INDEX "ix_ai_usage_logs_org_id_created_at" ON "ai_usage_logs" USING btree("org_id", "created_at");

-- changeset seongha.moon:1773067471545-12 splitStatements:false
CREATE INDEX "ix_email_verifications_email" ON "email_verifications" USING btree("email");

-- changeset seongha.moon:1773067471545-13 splitStatements:false
CREATE INDEX "ix_invitations_org_id" ON "invitations" USING btree("org_id");

-- changeset seongha.moon:1773067471545-14 splitStatements:false
CREATE INDEX "ix_invitations_invited_by" ON "invitations" USING btree("invited_by");

-- changeset seongha.moon:1773067471545-15 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "uq_invitations_org_id_email" UNIQUE ("org_id", "email");

-- changeset seongha.moon:1773067471545-16 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "uq_invitations_token_hash" UNIQUE ("token_hash");

-- changeset seongha.moon:1773067471545-17 splitStatements:false
CREATE INDEX "ix_memberships_org_id" ON "memberships" USING btree("org_id");

-- changeset seongha.moon:1773067471545-18 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "uq_memberships_user_id_org_id" UNIQUE ("user_id", "org_id");

-- changeset seongha.moon:1773067471545-19 splitStatements:false
CREATE INDEX "ix_organizations_owner_id" ON "organizations" USING btree("owner_id");

-- changeset seongha.moon:1773067471545-20 splitStatements:false
ALTER TABLE "organizations" ADD CONSTRAINT "organizations_slug_key" UNIQUE ("slug");

-- changeset seongha.moon:1773067471545-21 splitStatements:false
CREATE INDEX "ix_refresh_tokens_user_id" ON "refresh_tokens" USING btree("user_id");

-- changeset seongha.moon:1773067471545-22 splitStatements:false
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "uq_refresh_tokens_token_jti" UNIQUE ("token_jti");

-- changeset seongha.moon:1773067471545-23 splitStatements:false
CREATE INDEX "ix_subscriptions_org_id" ON "subscriptions" USING btree("org_id");

-- changeset seongha.moon:1773067471545-24 splitStatements:false
ALTER TABLE "users" ADD CONSTRAINT "uq_users_email" UNIQUE ("email");

-- changeset seongha.moon:1773067471545-25 splitStatements:false
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "fk1lih5y2npsf8u5o3vhdb9y0os" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-26 splitStatements:false
ALTER TABLE "organizations" ADD CONSTRAINT "fk525ovcw3fy6440s4o0tj8xr95" FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-27 splitStatements:false
ALTER TABLE "ai_usage_logs" ADD CONSTRAINT "fk_ai_usage_logs_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-28 splitStatements:false
ALTER TABLE "ai_usage_logs" ADD CONSTRAINT "fk_ai_usage_logs_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-29 splitStatements:false
ALTER TABLE "subscriptions" ADD CONSTRAINT "fk_subscriptions_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-30 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "fkdjormybfoo7f4i4d4r803qohb" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-31 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "fkh67axu8o0vump4ii8d89e2244" FOREIGN KEY ("invited_by") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-32 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "fkm695caclph8102p6wi3a4wecb" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset seongha.moon:1773067471545-33 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "fkmegko9cv4l2g9kl2rlbgb4yyq" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

