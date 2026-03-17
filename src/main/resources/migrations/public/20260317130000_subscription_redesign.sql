-- liquibase formatted sql

-- changeset codex:20260317130000-1 splitStatements:false
DROP TABLE IF EXISTS "storage_overage_ledgers" CASCADE;

-- changeset codex:20260317130000-2 splitStatements:false
DROP TABLE IF EXISTS "storage_usage_snapshots" CASCADE;

-- changeset codex:20260317130000-3 splitStatements:false
DROP TABLE IF EXISTS "subscription_billing_ledgers" CASCADE;

-- changeset codex:20260317130000-4 splitStatements:false
DROP TABLE IF EXISTS "subscription_credit_purchases" CASCADE;

-- changeset codex:20260317130000-5 splitStatements:false
DROP TABLE IF EXISTS "subscription_usage_policies" CASCADE;

-- changeset codex:20260317130000-6 splitStatements:false
DROP TABLE IF EXISTS "subscription_seat_assignments" CASCADE;

-- changeset codex:20260317130000-7 splitStatements:false
DROP TABLE IF EXISTS "subscription_seat_quotas" CASCADE;

-- changeset codex:20260317130000-8 splitStatements:false
DROP TABLE IF EXISTS "subscription_change_requests" CASCADE;

-- changeset codex:20260317130000-9 splitStatements:false
DROP TABLE IF EXISTS "subscriptions" CASCADE;

-- changeset codex:20260317130000-9a splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "allow_storage_overage";

-- changeset codex:20260317130000-9b splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "bonus_credits_remaining";

-- changeset codex:20260317130000-9c splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "max_members";

-- changeset codex:20260317130000-9d splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "plan_credits_remaining";

-- changeset codex:20260317130000-9e splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "plan_type";

-- changeset codex:20260317130000-9f splitStatements:false
ALTER TABLE "organizations" DROP COLUMN IF EXISTS "storage_bytes_limit";

-- changeset codex:20260317130000-10 splitStatements:false
CREATE TABLE "subscriptions" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "org_id" UUID NOT NULL,
    "plan_type" VARCHAR(20) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "billing_cycle" VARCHAR(20) NOT NULL,
    "current_period_start" TIMESTAMP WITH TIME ZONE NOT NULL,
    "current_period_end" TIMESTAMP WITH TIME ZONE NOT NULL,
    "scheduled_plan_type" VARCHAR(20),
    "scheduled_change_effective_at" TIMESTAMP WITH TIME ZONE,
    "cancel_at_period_end" BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT "subscriptions_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_subscriptions_org_id" UNIQUE ("org_id"),
    CONSTRAINT "ck_subscriptions_plan_type" CHECK ("plan_type" IN ('STARTER', 'TEAM', 'ORG', 'ENTERPRISE')),
    CONSTRAINT "ck_subscriptions_status" CHECK ("status" IN ('ACTIVE', 'PAST_DUE', 'CANCELED', 'EXPIRED')),
    CONSTRAINT "ck_subscriptions_billing_cycle" CHECK ("billing_cycle" IN ('MONTHLY', 'YEARLY')),
    CONSTRAINT "ck_subscriptions_scheduled_plan_type" CHECK ("scheduled_plan_type" IS NULL OR "scheduled_plan_type" IN ('STARTER', 'TEAM', 'ORG', 'ENTERPRISE')),
    CONSTRAINT "ck_subscriptions_period" CHECK ("current_period_end" > "current_period_start"),
    CONSTRAINT "fk_subscriptions_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-11 splitStatements:false
CREATE INDEX "ix_subscriptions_plan_type_status" ON "subscriptions" USING btree("plan_type", "status");

-- changeset codex:20260317130000-12 splitStatements:false
CREATE TABLE "subscription_seat_quotas" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "seat_type" VARCHAR(20) NOT NULL,
    "purchased_quantity" INTEGER NOT NULL,
    "unit_price" INTEGER NOT NULL,
    "currency" VARCHAR(3) NOT NULL,
    CONSTRAINT "subscription_seat_quotas_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_subscription_seat_quotas_subscription_id_seat_type" UNIQUE ("subscription_id", "seat_type"),
    CONSTRAINT "ck_subscription_seat_quotas_seat_type" CHECK ("seat_type" IN ('STARTER', 'VIEWER', 'COLLABORATOR', 'FULL')),
    CONSTRAINT "ck_subscription_seat_quotas_purchased_quantity" CHECK ("purchased_quantity" >= 0),
    CONSTRAINT "ck_subscription_seat_quotas_unit_price" CHECK ("unit_price" >= 0),
    CONSTRAINT "fk_subscription_seat_quotas_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-13 splitStatements:false
CREATE INDEX "ix_subscription_seat_quotas_subscription_id" ON "subscription_seat_quotas" USING btree("subscription_id");

-- changeset codex:20260317130000-14 splitStatements:false
CREATE TABLE "subscription_seat_assignments" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "org_id" UUID NOT NULL,
    "membership_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "seat_type" VARCHAR(20) NOT NULL,
    "assigned_by" UUID,
    "assigned_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "subscription_seat_assignments_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_subscription_seat_assignments_membership_id" UNIQUE ("membership_id"),
    CONSTRAINT "uq_subscription_seat_assignments_subscription_id_user_id" UNIQUE ("subscription_id", "user_id"),
    CONSTRAINT "ck_subscription_seat_assignments_seat_type" CHECK ("seat_type" IN ('STARTER', 'VIEWER', 'COLLABORATOR', 'FULL')),
    CONSTRAINT "fk_subscription_seat_assignments_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_seat_assignments_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_seat_assignments_membership_id" FOREIGN KEY ("membership_id") REFERENCES "memberships" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_seat_assignments_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_seat_assignments_assigned_by" FOREIGN KEY ("assigned_by") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE SET NULL
);

-- changeset codex:20260317130000-15 splitStatements:false
CREATE INDEX "ix_subscription_seat_assignments_org_id_seat_type" ON "subscription_seat_assignments" USING btree("org_id", "seat_type");

-- changeset codex:20260317130000-16 splitStatements:false
CREATE INDEX "ix_subscription_seat_assignments_subscription_id" ON "subscription_seat_assignments" USING btree("subscription_id");

-- changeset codex:20260317130000-17 splitStatements:false
CREATE TABLE "subscription_usage_policies" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "base_storage_bytes" BIGINT NOT NULL,
    "extra_storage_bytes_per_full_seat" BIGINT NOT NULL,
    "storage_overage_unit_bytes" BIGINT NOT NULL,
    "storage_overage_unit_price" NUMERIC(12, 2) NOT NULL,
    "starter_monthly_ai_credits" NUMERIC(12, 4) NOT NULL,
    "ai_billing_mode" VARCHAR(20) NOT NULL,
    "ai_monthly_credit_limit" NUMERIC(12, 4),
    "ai_hard_limit_enabled" BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT "subscription_usage_policies_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_subscription_usage_policies_subscription_id" UNIQUE ("subscription_id"),
    CONSTRAINT "ck_subscription_usage_policies_base_storage_bytes" CHECK ("base_storage_bytes" >= 0),
    CONSTRAINT "ck_subscription_usage_policies_extra_storage_bytes_per_full_seat" CHECK ("extra_storage_bytes_per_full_seat" >= 0),
    CONSTRAINT "ck_subscription_usage_policies_storage_overage_unit_bytes" CHECK ("storage_overage_unit_bytes" > 0),
    CONSTRAINT "ck_subscription_usage_policies_storage_overage_unit_price" CHECK ("storage_overage_unit_price" >= 0),
    CONSTRAINT "ck_subscription_usage_policies_starter_monthly_ai_credits" CHECK ("starter_monthly_ai_credits" >= 0),
    CONSTRAINT "ck_subscription_usage_policies_ai_billing_mode" CHECK ("ai_billing_mode" IN ('INCLUDED_ONLY', 'METERED')),
    CONSTRAINT "ck_subscription_usage_policies_ai_monthly_credit_limit" CHECK ("ai_monthly_credit_limit" IS NULL OR "ai_monthly_credit_limit" >= 0),
    CONSTRAINT "fk_subscription_usage_policies_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-18 splitStatements:false
CREATE TABLE "subscription_credit_purchases" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "org_id" UUID NOT NULL,
    "credits_purchased" NUMERIC(12, 4) NOT NULL,
    "credits_remaining" NUMERIC(12, 4) NOT NULL,
    "unit_price" NUMERIC(12, 4) NOT NULL,
    "total_amount" NUMERIC(12, 2) NOT NULL,
    "currency" VARCHAR(3) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "expires_at" TIMESTAMP WITH TIME ZONE,
    CONSTRAINT "subscription_credit_purchases_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_subscription_credit_purchases_credits_purchased" CHECK ("credits_purchased" > 0),
    CONSTRAINT "ck_subscription_credit_purchases_credits_remaining" CHECK ("credits_remaining" >= 0),
    CONSTRAINT "ck_subscription_credit_purchases_unit_price" CHECK ("unit_price" >= 0),
    CONSTRAINT "ck_subscription_credit_purchases_total_amount" CHECK ("total_amount" >= 0),
    CONSTRAINT "ck_subscription_credit_purchases_status" CHECK ("status" IN ('ACTIVE', 'EXHAUSTED', 'EXPIRED', 'CANCELED')),
    CONSTRAINT "fk_subscription_credit_purchases_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_credit_purchases_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-19 splitStatements:false
CREATE INDEX "ix_subscription_credit_purchases_org_id_status" ON "subscription_credit_purchases" USING btree("org_id", "status");

-- changeset codex:20260317130000-20 splitStatements:false
CREATE TABLE "subscription_billing_ledgers" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "org_id" UUID NOT NULL,
    "ledger_type" VARCHAR(30) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "period_start" TIMESTAMP WITH TIME ZONE,
    "period_end" TIMESTAMP WITH TIME ZONE,
    "quantity" NUMERIC(19, 6) NOT NULL,
    "unit_amount" NUMERIC(12, 2) NOT NULL,
    "total_amount" NUMERIC(12, 2) NOT NULL,
    "currency" VARCHAR(3) NOT NULL,
    "reference_type" VARCHAR(30),
    "reference_id" UUID,
    "metadata" JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT "subscription_billing_ledgers_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_subscription_billing_ledgers_quantity" CHECK ("quantity" >= 0),
    CONSTRAINT "ck_subscription_billing_ledgers_unit_amount" CHECK ("unit_amount" >= 0),
    CONSTRAINT "ck_subscription_billing_ledgers_total_amount" CHECK ("total_amount" >= 0),
    CONSTRAINT "ck_subscription_billing_ledgers_ledger_type" CHECK ("ledger_type" IN ('BASE_SUBSCRIPTION', 'SEAT', 'AI_USAGE', 'AI_CREDIT_PURCHASE', 'STORAGE_OVERAGE', 'ADJUSTMENT')),
    CONSTRAINT "ck_subscription_billing_ledgers_status" CHECK ("status" IN ('PENDING', 'INVOICED', 'SETTLED', 'VOIDED')),
    CONSTRAINT "fk_subscription_billing_ledgers_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_subscription_billing_ledgers_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-21 splitStatements:false
CREATE INDEX "ix_subscription_billing_ledgers_org_id_period_start" ON "subscription_billing_ledgers" USING btree("org_id", "period_start");

-- changeset codex:20260317130000-22 splitStatements:false
CREATE INDEX "ix_subscription_billing_ledgers_subscription_id_status" ON "subscription_billing_ledgers" USING btree("subscription_id", "status");

-- changeset codex:20260317130000-23 splitStatements:false
CREATE TABLE "storage_usage_snapshots" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "org_id" UUID NOT NULL,
    "snapshot_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "total_file_bytes" BIGINT NOT NULL,
    "included_bytes" BIGINT NOT NULL,
    "billable_bytes" BIGINT NOT NULL,
    "overage_bytes" BIGINT NOT NULL,
    "metadata" JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT "storage_usage_snapshots_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_storage_usage_snapshots_org_id_snapshot_at" UNIQUE ("org_id", "snapshot_at"),
    CONSTRAINT "ck_storage_usage_snapshots_total_file_bytes" CHECK ("total_file_bytes" >= 0),
    CONSTRAINT "ck_storage_usage_snapshots_included_bytes" CHECK ("included_bytes" >= 0),
    CONSTRAINT "ck_storage_usage_snapshots_billable_bytes" CHECK ("billable_bytes" >= 0),
    CONSTRAINT "ck_storage_usage_snapshots_overage_bytes" CHECK ("overage_bytes" >= 0),
    CONSTRAINT "fk_storage_usage_snapshots_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-24 splitStatements:false
CREATE INDEX "ix_storage_usage_snapshots_org_id_created_at" ON "storage_usage_snapshots" USING btree("org_id", "created_at");

-- changeset codex:20260317130000-25 splitStatements:false
CREATE TABLE "storage_overage_ledgers" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "org_id" UUID NOT NULL,
    "snapshot_id" UUID,
    "period_start" TIMESTAMP WITH TIME ZONE NOT NULL,
    "period_end" TIMESTAMP WITH TIME ZONE NOT NULL,
    "overage_bytes" BIGINT NOT NULL,
    "billable_gb" NUMERIC(19, 6) NOT NULL,
    "unit_price" NUMERIC(12, 2) NOT NULL,
    "total_amount" NUMERIC(12, 2) NOT NULL,
    "currency" VARCHAR(3) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    CONSTRAINT "storage_overage_ledgers_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_storage_overage_ledgers_period" CHECK ("period_end" > "period_start"),
    CONSTRAINT "ck_storage_overage_ledgers_overage_bytes" CHECK ("overage_bytes" >= 0),
    CONSTRAINT "ck_storage_overage_ledgers_billable_gb" CHECK ("billable_gb" >= 0),
    CONSTRAINT "ck_storage_overage_ledgers_unit_price" CHECK ("unit_price" >= 0),
    CONSTRAINT "ck_storage_overage_ledgers_total_amount" CHECK ("total_amount" >= 0),
    CONSTRAINT "ck_storage_overage_ledgers_status" CHECK ("status" IN ('PENDING', 'INVOICED', 'SETTLED', 'VOIDED')),
    CONSTRAINT "fk_storage_overage_ledgers_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_storage_overage_ledgers_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_storage_overage_ledgers_snapshot_id" FOREIGN KEY ("snapshot_id") REFERENCES "storage_usage_snapshots" ("id") ON UPDATE NO ACTION ON DELETE SET NULL
);

-- changeset codex:20260317130000-26 splitStatements:false
CREATE INDEX "ix_storage_overage_ledgers_org_id_period_start" ON "storage_overage_ledgers" USING btree("org_id", "period_start");

-- changeset codex:20260317130000-27 splitStatements:false
CREATE TABLE "subscription_change_requests" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "subscription_id" UUID NOT NULL,
    "requested_plan_type" VARCHAR(20),
    "effective_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "metadata" JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT "subscription_change_requests_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_subscription_change_requests_requested_plan_type" CHECK ("requested_plan_type" IS NULL OR "requested_plan_type" IN ('STARTER', 'TEAM', 'ORG', 'ENTERPRISE')),
    CONSTRAINT "ck_subscription_change_requests_status" CHECK ("status" IN ('SCHEDULED', 'APPLIED', 'CANCELED', 'FAILED')),
    CONSTRAINT "fk_subscription_change_requests_subscription_id" FOREIGN KEY ("subscription_id") REFERENCES "subscriptions" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130000-28 splitStatements:false
CREATE INDEX "ix_subscription_change_requests_subscription_id_status" ON "subscription_change_requests" USING btree("subscription_id", "status");

-- changeset codex:20260317130000-29 splitStatements:false
ALTER TABLE "organizations"
    DROP COLUMN IF EXISTS "plan_type",
    DROP COLUMN IF EXISTS "max_members",
    DROP COLUMN IF EXISTS "plan_credits_remaining",
    DROP COLUMN IF EXISTS "bonus_credits_remaining",
    DROP COLUMN IF EXISTS "storage_bytes_limit",
    DROP COLUMN IF EXISTS "storage_bytes_used",
    DROP COLUMN IF EXISTS "allow_storage_overage",
    DROP COLUMN IF EXISTS "used_members";
