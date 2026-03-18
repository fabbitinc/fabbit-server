-- liquibase formatted sql

-- changeset codex:20260317120804-1 splitStatements:false
CREATE TABLE "users" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "hashed_password" VARCHAR(255) NOT NULL,
    "full_name" VARCHAR(100) NOT NULL,
    "phone" VARCHAR(20),
    "profile_image_file_key" VARCHAR(1000),
    "is_active" BOOLEAN NOT NULL,
    CONSTRAINT "users_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_users_email" UNIQUE ("email")
);

-- changeset codex:20260317120804-2 splitStatements:false
CREATE TABLE "email_verifications" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "code_hash" VARCHAR(64) NOT NULL,
    "verification_token_hash" VARCHAR(64),
    "status" VARCHAR(20) NOT NULL,
    "attempt_count" INTEGER NOT NULL,
    "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT "email_verifications_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "ck_email_verifications_status" CHECK ("status" IN ('PENDING', 'VERIFIED', 'USED'))
);

-- changeset codex:20260317120804-3 splitStatements:false
CREATE INDEX "ix_email_verifications_email" ON "email_verifications" USING btree("email");

-- changeset codex:20260317120804-4 splitStatements:false
CREATE TABLE "organizations" (
    "id" UUID NOT NULL,
    "slug" VARCHAR(50) NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "owner_id" UUID NOT NULL,
    "industry" VARCHAR(50),
    "team_size" VARCHAR(20),
    "storage_bytes_used" BIGINT NOT NULL,
    "used_members" INTEGER NOT NULL,
    "profile_image_file_key" VARCHAR(255),
    CONSTRAINT "organizations_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_organizations_slug" UNIQUE ("slug"),
    CONSTRAINT "fk_organizations_owner_id" FOREIGN KEY ("owner_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- changeset codex:20260317120804-5 splitStatements:false
CREATE INDEX "ix_organizations_owner_id" ON "organizations" USING btree("owner_id");

-- changeset codex:20260317120804-6 splitStatements:false
CREATE TABLE "memberships" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "user_id" UUID NOT NULL,
    "org_id" UUID NOT NULL,
    "role" VARCHAR(20) NOT NULL,
    "job_role" VARCHAR(50),
    CONSTRAINT "memberships_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_memberships_user_id_org_id" UNIQUE ("user_id", "org_id"),
    CONSTRAINT "ck_memberships_role" CHECK ("role" IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT "fk_memberships_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT "fk_memberships_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- changeset codex:20260317120804-7 splitStatements:false
CREATE INDEX "ix_memberships_org_id" ON "memberships" USING btree("org_id");

-- changeset codex:20260317120804-8 splitStatements:false
CREATE TABLE "invitations" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "org_id" UUID NOT NULL,
    "email" VARCHAR(255) NOT NULL,
    "role" VARCHAR(20) NOT NULL,
    "seat_type" VARCHAR(20) NOT NULL,
    "token_hash" VARCHAR(64) NOT NULL,
    "status" VARCHAR(20) NOT NULL,
    "invited_by" UUID NOT NULL,
    "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "accepted_at" TIMESTAMP WITH TIME ZONE,
    CONSTRAINT "invitations_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_invitations_org_id_email" UNIQUE ("org_id", "email"),
    CONSTRAINT "uq_invitations_token_hash" UNIQUE ("token_hash"),
    CONSTRAINT "ck_invitations_role" CHECK ("role" IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT "ck_invitations_seat_type" CHECK ("seat_type" IN ('STARTER', 'VIEWER', 'COLLABORATOR', 'FULL')),
    CONSTRAINT "ck_invitations_status" CHECK ("status" IN ('PENDING', 'ACCEPTED', 'CANCELLED')),
    CONSTRAINT "fk_invitations_org_id" FOREIGN KEY ("org_id") REFERENCES "organizations" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION,
    CONSTRAINT "fk_invitations_invited_by" FOREIGN KEY ("invited_by") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- changeset codex:20260317120804-9 splitStatements:false
CREATE INDEX "ix_invitations_org_id" ON "invitations" USING btree("org_id");

-- changeset codex:20260317120804-10 splitStatements:false
CREATE INDEX "ix_invitations_invited_by" ON "invitations" USING btree("invited_by");

-- changeset codex:20260317120804-11 splitStatements:false
CREATE INDEX "ix_invitations_org_id_seat_type_status" ON "invitations" USING btree("org_id", "seat_type", "status");

-- changeset codex:20260317120804-12 splitStatements:false
CREATE TABLE "refresh_tokens" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "user_id" UUID NOT NULL,
    "token_jti" VARCHAR(36) NOT NULL,
    "expires_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "revoked_at" TIMESTAMP WITH TIME ZONE,
    CONSTRAINT "refresh_tokens_pkey" PRIMARY KEY ("id"),
    CONSTRAINT "uq_refresh_tokens_token_jti" UNIQUE ("token_jti"),
    CONSTRAINT "fk_refresh_tokens_user_id" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION
);

-- changeset codex:20260317120804-13 splitStatements:false
CREATE INDEX "ix_refresh_tokens_user_id" ON "refresh_tokens" USING btree("user_id");
