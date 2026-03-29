-- liquibase formatted sql

-- changeset moonseongha:20260329120000-1 splitStatements:false
CREATE TABLE "part_number_categories" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    "prefix" VARCHAR(20) NOT NULL,
    "delimiter" VARCHAR(5) NOT NULL DEFAULT '-',
    "digits" INTEGER NOT NULL DEFAULT 4,
    CONSTRAINT "part_number_categories_pkey" PRIMARY KEY ("id")
);

-- changeset moonseongha:20260329120000-2 splitStatements:false
ALTER TABLE "part_number_categories" ADD CONSTRAINT "uq_part_number_categories_name" UNIQUE ("name");

-- changeset moonseongha:20260329120000-3 splitStatements:false
ALTER TABLE "part_number_categories" ADD CONSTRAINT "uq_part_number_categories_prefix" UNIQUE ("prefix");

-- changeset moonseongha:20260329120000-4 splitStatements:false
CREATE TABLE "part_number_sequences" (
    "id" UUID NOT NULL,
    "created_at" TIMESTAMP WITH TIME ZONE NOT NULL,
    "category_id" UUID NOT NULL,
    "current_value" INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT "part_number_sequences_pkey" PRIMARY KEY ("id")
);

-- changeset moonseongha:20260329120000-5 splitStatements:false
ALTER TABLE "part_number_sequences" ADD CONSTRAINT "uq_part_number_sequences_category_id" UNIQUE ("category_id");

-- changeset moonseongha:20260329120000-6 splitStatements:false
ALTER TABLE "part_number_sequences" ADD CONSTRAINT "fk_part_number_sequences_category_id"
    FOREIGN KEY ("category_id") REFERENCES "part_number_categories" ("id");

-- changeset moonseongha:20260329120000-7 splitStatements:false
ALTER TABLE "parts" ADD COLUMN "item_type" VARCHAR(30);

-- changeset moonseongha:20260329120000-8 splitStatements:false
ALTER TABLE "parts" ADD COLUMN "numbering_category_id" UUID;

-- changeset moonseongha:20260329120000-9 splitStatements:false
ALTER TABLE "parts" ADD CONSTRAINT "fk_parts_numbering_category_id"
    FOREIGN KEY ("numbering_category_id") REFERENCES "part_number_categories" ("id");

-- changeset moonseongha:20260329120000-10 splitStatements:false
ALTER TABLE "part_revisions" DROP COLUMN IF EXISTS "is_phantom";

-- changeset moonseongha:20260329120000-11 splitStatements:false
ALTER TABLE "part_revisions" DROP COLUMN IF EXISTS "category";
