-- liquibase formatted sql

-- changeset moonseongha:1775415881858-1 splitStatements:false
CREATE TABLE "email_verifications" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "attempt_count" INTEGER NOT NULL, "code_hash" VARCHAR(64) NOT NULL, "email" VARCHAR(255) NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "status" VARCHAR(20) NOT NULL, "verification_token_hash" VARCHAR(64), CONSTRAINT "email_verifications_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-2 splitStatements:false
CREATE TABLE "invitations" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "accepted_at" TIMESTAMP WITH TIME ZONE, "email" VARCHAR(255) NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "invited_by" UUID NOT NULL, "org_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "seat_type" VARCHAR(20) NOT NULL, "status" VARCHAR(20) NOT NULL, "token_hash" VARCHAR(64) NOT NULL, CONSTRAINT "invitations_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-3 splitStatements:false
CREATE TABLE "memberships" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "job_role" VARCHAR(50), "org_id" UUID NOT NULL, "role" VARCHAR(20) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "memberships_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-4 splitStatements:false
CREATE TABLE "organizations" ("id" UUID NOT NULL, "industry" VARCHAR(50), "name" VARCHAR(100) NOT NULL, "owner_id" UUID NOT NULL, "profile_image_file_key" VARCHAR(255), "slug" VARCHAR(50) NOT NULL, "storage_bytes_used" BIGINT NOT NULL, "team_size" VARCHAR(20), "used_members" INTEGER NOT NULL, CONSTRAINT "organizations_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-5 splitStatements:false
CREATE TABLE "refresh_tokens" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL, "revoked_at" TIMESTAMP WITH TIME ZONE, "token_jti" VARCHAR(36) NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-6 splitStatements:false
CREATE TABLE "subscription_seat_quotas" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "currency" VARCHAR(3) NOT NULL, "purchased_quantity" INTEGER NOT NULL, "seat_type" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, "unit_price" INTEGER NOT NULL, CONSTRAINT "subscription_seat_quotas_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-7 splitStatements:false
CREATE TABLE "subscriptions" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "billing_cycle" VARCHAR(20) NOT NULL, "cancel_at_period_end" BOOLEAN NOT NULL, "current_period_end" TIMESTAMP WITH TIME ZONE NOT NULL, "current_period_start" TIMESTAMP WITH TIME ZONE NOT NULL, "org_id" UUID NOT NULL, "plan_type" VARCHAR(20) NOT NULL, "scheduled_change_effective_at" TIMESTAMP WITH TIME ZONE, "scheduled_plan_type" VARCHAR(20), "status" VARCHAR(20) NOT NULL, CONSTRAINT "subscriptions_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-8 splitStatements:false
CREATE TABLE "storage_overage_ledgers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "billable_gb" numeric(19, 6) NOT NULL, "currency" VARCHAR(3) NOT NULL, "org_id" UUID NOT NULL, "overage_bytes" BIGINT NOT NULL, "period_end" TIMESTAMP WITH TIME ZONE NOT NULL, "period_start" TIMESTAMP WITH TIME ZONE NOT NULL, "snapshot_id" UUID, "status" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, "total_amount" numeric(12, 2) NOT NULL, "unit_price" numeric(12, 2) NOT NULL, CONSTRAINT "storage_overage_ledgers_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-9 splitStatements:false
CREATE TABLE "subscription_billing_ledgers" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "currency" VARCHAR(3) NOT NULL, "ledger_type" VARCHAR(30) NOT NULL, "metadata" JSONB NOT NULL, "org_id" UUID NOT NULL, "period_end" TIMESTAMP WITH TIME ZONE, "period_start" TIMESTAMP WITH TIME ZONE, "quantity" numeric(19, 6) NOT NULL, "reference_id" UUID, "reference_type" VARCHAR(30), "status" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, "total_amount" numeric(12, 2) NOT NULL, "unit_amount" numeric(12, 2) NOT NULL, CONSTRAINT "subscription_billing_ledgers_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-10 splitStatements:false
CREATE TABLE "subscription_change_requests" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "effective_at" TIMESTAMP WITH TIME ZONE NOT NULL, "metadata" JSONB NOT NULL, "requested_plan_type" VARCHAR(20), "status" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, CONSTRAINT "subscription_change_requests_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-11 splitStatements:false
CREATE TABLE "subscription_credit_purchases" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "credits_purchased" numeric(12, 4) NOT NULL, "credits_remaining" numeric(12, 4) NOT NULL, "currency" VARCHAR(3) NOT NULL, "expires_at" TIMESTAMP WITH TIME ZONE, "org_id" UUID NOT NULL, "status" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, "total_amount" numeric(12, 2) NOT NULL, "unit_price" numeric(12, 4) NOT NULL, CONSTRAINT "subscription_credit_purchases_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-12 splitStatements:false
CREATE TABLE "subscription_seat_assignments" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "assigned_at" TIMESTAMP WITH TIME ZONE NOT NULL, "assigned_by" UUID, "membership_id" UUID NOT NULL, "org_id" UUID NOT NULL, "seat_type" VARCHAR(20) NOT NULL, "subscription_id" UUID NOT NULL, "user_id" UUID NOT NULL, CONSTRAINT "subscription_seat_assignments_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-13 splitStatements:false
CREATE TABLE "subscription_usage_policies" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "ai_billing_mode" VARCHAR(20) NOT NULL, "ai_hard_limit_enabled" BOOLEAN NOT NULL, "ai_monthly_credit_limit" numeric(12, 4), "base_storage_bytes" BIGINT NOT NULL, "extra_storage_bytes_per_full_seat" BIGINT NOT NULL, "starter_monthly_ai_credits" numeric(12, 4) NOT NULL, "storage_overage_unit_bytes" BIGINT NOT NULL, "storage_overage_unit_price" numeric(12, 2) NOT NULL, "subscription_id" UUID NOT NULL, CONSTRAINT "subscription_usage_policies_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-14 splitStatements:false
CREATE TABLE "storage_usage_snapshots" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "billable_bytes" BIGINT NOT NULL, "included_bytes" BIGINT NOT NULL, "metadata" JSONB NOT NULL, "org_id" UUID NOT NULL, "overage_bytes" BIGINT NOT NULL, "snapshot_at" TIMESTAMP WITH TIME ZONE NOT NULL, "total_file_bytes" BIGINT NOT NULL, CONSTRAINT "storage_usage_snapshots_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-15 splitStatements:false
CREATE TABLE "users" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "is_active" BOOLEAN NOT NULL, "email" VARCHAR(255) NOT NULL, "full_name" VARCHAR(100) NOT NULL, "hashed_password" VARCHAR(255) NOT NULL, "phone" VARCHAR(20), "profile_image_file_key" VARCHAR(1000), CONSTRAINT "users_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775415881858-16 splitStatements:false
CREATE INDEX "ix_email_verifications_email" ON "email_verifications" USING btree("email");

-- changeset moonseongha:1775415881858-17 splitStatements:false
CREATE INDEX "ix_invitations_org_id" ON "invitations" USING btree("org_id");

-- changeset moonseongha:1775415881858-18 splitStatements:false
CREATE INDEX "ix_invitations_invited_by" ON "invitations" USING btree("invited_by");

-- changeset moonseongha:1775415881858-19 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "uq_invitations_org_id_email" UNIQUE ("org_id", "email");

-- changeset moonseongha:1775415881858-20 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "uq_invitations_token_hash" UNIQUE ("token_hash");

-- changeset moonseongha:1775415881858-21 splitStatements:false
CREATE INDEX "ix_memberships_org_id" ON "memberships" USING btree("org_id");

-- changeset moonseongha:1775415881858-22 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "uq_memberships_user_id_org_id" UNIQUE ("user_id", "org_id");

-- changeset moonseongha:1775415881858-23 splitStatements:false
CREATE INDEX "ix_organizations_owner_id" ON "organizations" USING btree("owner_id");

-- changeset moonseongha:1775415881858-24 splitStatements:false
ALTER TABLE "organizations" ADD CONSTRAINT "organizations_slug_key" UNIQUE ("slug");

-- changeset moonseongha:1775415881858-25 splitStatements:false
CREATE INDEX "ix_refresh_tokens_user_id" ON "refresh_tokens" USING btree("user_id");

-- changeset moonseongha:1775415881858-26 splitStatements:false
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "uq_refresh_tokens_token_jti" UNIQUE ("token_jti");

-- changeset moonseongha:1775415881858-27 splitStatements:false
CREATE INDEX "ix_subscription_seat_quotas_subscription_id" ON "subscription_seat_quotas" USING btree("subscription_id");

-- changeset moonseongha:1775415881858-28 splitStatements:false
ALTER TABLE "subscription_seat_quotas" ADD CONSTRAINT "uq_subscription_seat_quotas_subscription_id_seat_type" UNIQUE ("subscription_id", "seat_type");

-- changeset moonseongha:1775415881858-29 splitStatements:false
CREATE INDEX "ix_subscriptions_org_id" ON "subscriptions" USING btree("org_id");

-- changeset moonseongha:1775415881858-30 splitStatements:false
CREATE INDEX "ix_subscriptions_plan_type_status" ON "subscriptions" USING btree("plan_type", "status");

-- changeset moonseongha:1775415881858-31 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "uq_subscription_seat_assignments_membership_id" UNIQUE ("membership_id");

-- changeset moonseongha:1775415881858-32 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "uq_subscription_seat_assignments_subscription_id_user_id" UNIQUE ("subscription_id", "user_id");

-- changeset moonseongha:1775415881858-33 splitStatements:false
ALTER TABLE "subscription_usage_policies" ADD CONSTRAINT "uq_subscription_usage_policies_subscription_id" UNIQUE ("subscription_id");

-- changeset moonseongha:1775415881858-34 splitStatements:false
ALTER TABLE "users" ADD CONSTRAINT "uq_users_email" UNIQUE ("email");

-- changeset moonseongha:1775415881858-35 splitStatements:false
ALTER TABLE "refresh_tokens" ADD CONSTRAINT "fk1lih5y2npsf8u5o3vhdb9y0os" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-36 splitStatements:false
ALTER TABLE "organizations" ADD CONSTRAINT "fk525ovcw3fy6440s4o0tj8xr95" FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-37 splitStatements:false
ALTER TABLE "storage_overage_ledgers" ADD CONSTRAINT "fk_storage_overage_ledgers_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-38 splitStatements:false
ALTER TABLE "subscription_billing_ledgers" ADD CONSTRAINT "fk_subscription_billing_ledgers_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-39 splitStatements:false
ALTER TABLE "subscription_change_requests" ADD CONSTRAINT "fk_subscription_change_requests_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-40 splitStatements:false
ALTER TABLE "subscription_credit_purchases" ADD CONSTRAINT "fk_subscription_credit_purchases_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-41 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "fk_subscription_seat_assignments_membership_id" FOREIGN KEY ("membership_id") REFERENCES "memberships" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-42 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "fk_subscription_seat_assignments_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-43 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "fk_subscription_seat_assignments_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-44 splitStatements:false
ALTER TABLE "subscription_seat_assignments" ADD CONSTRAINT "fk_subscription_seat_assignments_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-45 splitStatements:false
ALTER TABLE "subscription_seat_quotas" ADD CONSTRAINT "fk_subscription_seat_quotas_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-46 splitStatements:false
ALTER TABLE "subscription_usage_policies" ADD CONSTRAINT "fk_subscription_usage_policies_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-47 splitStatements:false
ALTER TABLE "subscriptions" ADD CONSTRAINT "fk_subscriptions_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-48 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "fkdjormybfoo7f4i4d4r803qohb" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-49 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "fkh67axu8o0vump4ii8d89e2244" FOREIGN KEY ("invited_by") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-50 splitStatements:false
ALTER TABLE "memberships" ADD CONSTRAINT "fkm695caclph8102p6wi3a4wecb" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775415881858-51 splitStatements:false
ALTER TABLE "invitations" ADD CONSTRAINT "fkmegko9cv4l2g9kl2rlbgb4yyq" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

