-- liquibase formatted sql

-- changeset codex:20260314123000-1 splitStatements:false
ALTER TABLE "part_revisions" DROP CONSTRAINT IF EXISTS "fk5hquogokt71fc6i4j2tybt8rh";

-- changeset codex:20260314123000-2 splitStatements:false
ALTER TABLE "parts" DROP CONSTRAINT IF EXISTS "fki8is5lhkwmnm2cuca1t9joveb";

-- changeset codex:20260314123000-3 splitStatements:false
ALTER TABLE "part_revisions" DROP COLUMN IF EXISTS "drawing_id";

-- changeset codex:20260314123000-4 splitStatements:false
ALTER TABLE "parts" DROP COLUMN IF EXISTS "drawing_id";
