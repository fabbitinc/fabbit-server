ALTER TABLE "engineering_changes"
RENAME COLUMN "merged_at" TO "released_at";

ALTER TABLE "engineering_changes"
RENAME COLUMN "merged_by" TO "released_by";
