-- liquibase formatted sql

-- changeset seongha.moon:1773349076017-1 splitStatements:false
ALTER TABLE "files" ADD "content_hash" VARCHAR(64);

-- changeset seongha.moon:1773349076017-2 splitStatements:false
UPDATE "files"
SET "content_hash" = lpad(md5("id"::text), 64, '0')
WHERE "content_hash" IS NULL;

-- changeset seongha.moon:1773349076017-3 splitStatements:false
CREATE INDEX "ix_files_original_name_file_size_content_hash" ON "files" USING btree("original_name", "file_size", "content_hash");
