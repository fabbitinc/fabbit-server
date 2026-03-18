-- liquibase formatted sql

-- changeset codex:20260317130010-1 splitStatements:false
CREATE TABLE "ai_usage_events" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "org_id" UUID NOT NULL,
    "user_id" UUID NOT NULL,
    "plan_type_snapshot" VARCHAR(20) NOT NULL,
    "seat_type_snapshot" VARCHAR(20) NOT NULL,
    "category" VARCHAR(30) NOT NULL,
    "feature" VARCHAR(50) NOT NULL,
    "model" VARCHAR(50) NOT NULL,
    "input_tokens" INTEGER NOT NULL,
    "output_tokens" INTEGER NOT NULL,
    "credits_used" NUMERIC(12, 4) NOT NULL,
    "billable_amount" NUMERIC(12, 2) NOT NULL DEFAULT 0,
    "billing_status" VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    "metadata" JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT "ai_usage_events_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_ai_usage_events_plan_type_snapshot" CHECK ("plan_type_snapshot" IN ('STARTER', 'TEAM', 'ORG', 'ENTERPRISE')),
    CONSTRAINT "ck_ai_usage_events_seat_type_snapshot" CHECK ("seat_type_snapshot" IN ('STARTER', 'VIEWER', 'COLLABORATOR', 'FULL')),
    CONSTRAINT "ck_ai_usage_events_input_tokens" CHECK ("input_tokens" >= 0),
    CONSTRAINT "ck_ai_usage_events_output_tokens" CHECK ("output_tokens" >= 0),
    CONSTRAINT "ck_ai_usage_events_credits_used" CHECK ("credits_used" >= 0),
    CONSTRAINT "ck_ai_usage_events_billable_amount" CHECK ("billable_amount" >= 0),
    CONSTRAINT "ck_ai_usage_events_billing_status" CHECK ("billing_status" IN ('PENDING', 'BILLED', 'VOIDED')),
    CONSTRAINT "fk_ai_usage_events_org_id" FOREIGN KEY ("org_id") REFERENCES public."organizations" ("id") ON UPDATE NO ACTION ON DELETE CASCADE,
    CONSTRAINT "fk_ai_usage_events_user_id" FOREIGN KEY ("user_id") REFERENCES public."users" ("id") ON UPDATE NO ACTION ON DELETE CASCADE
);

-- changeset codex:20260317130010-2 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id" ON "ai_usage_events" USING btree("org_id");

-- changeset codex:20260317130010-3 splitStatements:false
CREATE INDEX "ix_ai_usage_events_user_id" ON "ai_usage_events" USING btree("user_id");

-- changeset codex:20260317130010-4 splitStatements:false
CREATE INDEX "ix_ai_usage_events_org_id_created_at" ON "ai_usage_events" USING btree("org_id", "created_at");
