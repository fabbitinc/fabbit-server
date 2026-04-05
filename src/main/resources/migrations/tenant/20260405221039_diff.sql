-- liquibase formatted sql

-- 1. 새 테이블 생성
-- changeset moonseongha:1775394647437-2 splitStatements:false
CREATE TABLE "engineering_change_step_stages" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "completion_policy" VARCHAR(30) NOT NULL, "deadline" TIMESTAMP WITH TIME ZONE, "engineering_change_id" UUID NOT NULL, "min_approvals" INTEGER, "sequence" INTEGER NOT NULL, "step_type" VARCHAR(30) NOT NULL, CONSTRAINT "engineering_change_step_stages_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775394647437-5 splitStatements:false
CREATE TABLE "workflow_templates" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "updated_at" TIMESTAMP WITH TIME ZONE NOT NULL, "created_by" UUID, "updated_by" UUID, "description" TEXT, "name" VARCHAR(200) NOT NULL, CONSTRAINT "workflow_templates_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775394647437-4 splitStatements:false
CREATE TABLE "workflow_template_stages" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "completion_policy" VARCHAR(30) NOT NULL, "min_approvals" INTEGER, "sequence" INTEGER NOT NULL, "step_type" VARCHAR(30) NOT NULL, "workflow_template_id" UUID NOT NULL, CONSTRAINT "workflow_template_stages_pkey" PRIMARY KEY ("id"));

-- changeset moonseongha:1775394647437-3 splitStatements:false
CREATE TABLE "workflow_template_stage_assignees" ("id" UUID NOT NULL, "created_at" TIMESTAMP WITH TIME ZONE NOT NULL, "assignee_id" UUID NOT NULL, "assignee_type" VARCHAR(20) NOT NULL, "workflow_template_stage_id" UUID NOT NULL, CONSTRAINT "workflow_template_stage_assignees_pkey" PRIMARY KEY ("id"));

-- 2. 기존 테이블 컬럼 추가/변경
-- changeset moonseongha:1775394647437-10 splitStatements:false
ALTER TABLE "engineering_change_steps" ADD "step_stage_id" UUID NOT NULL;

-- changeset moonseongha:1775394647437-11 splitStatements:false
ALTER TABLE "engineering_changes" ADD "version" BIGINT;

-- changeset moonseongha:1775394647437-1 splitStatements:false
ALTER TABLE "engineering_change_steps" ALTER COLUMN "status" TYPE VARCHAR(30) USING ("status"::VARCHAR(30));

-- 3. 인덱스 생성 (컬럼이 존재한 후)
-- changeset moonseongha:1775394647437-6 splitStatements:false
CREATE INDEX "ix_ec_step_stages_ec_id_seq" ON "engineering_change_step_stages" USING btree("engineering_change_id", "sequence");

-- changeset moonseongha:1775394647437-7 splitStatements:false
CREATE INDEX "ix_ec_steps_stage_id" ON "engineering_change_steps" USING btree("step_stage_id");

-- changeset moonseongha:1775394647437-8 splitStatements:false
CREATE INDEX "ix_wf_tpl_stage_assignees_stage_id" ON "workflow_template_stage_assignees" USING btree("workflow_template_stage_id");

-- changeset moonseongha:1775394647437-9 splitStatements:false
CREATE INDEX "ix_wf_tpl_stages_template_id" ON "workflow_template_stages" USING btree("workflow_template_id");

-- 4. FK 제약 조건
-- changeset moonseongha:1775394647437-13 splitStatements:false
ALTER TABLE "engineering_change_step_stages" ADD CONSTRAINT "fk64tikpktx4imyeqqsfjy1tjxo" FOREIGN KEY ("engineering_change_id") REFERENCES "engineering_changes" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775394647437-15 splitStatements:false
ALTER TABLE "engineering_change_steps" ADD CONSTRAINT "fkeu11iqmswadloyvjrx0ef9ofb" FOREIGN KEY ("step_stage_id") REFERENCES "engineering_change_step_stages" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775394647437-14 splitStatements:false
ALTER TABLE "workflow_template_stages" ADD CONSTRAINT "fk8720x6nq49ub9owsiag4vcud9" FOREIGN KEY ("workflow_template_id") REFERENCES "workflow_templates" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- changeset moonseongha:1775394647437-12 splitStatements:false
ALTER TABLE "workflow_template_stage_assignees" ADD CONSTRAINT "fk504yfhli4t9t4ne44ng27jvjq" FOREIGN KEY ("workflow_template_stage_id") REFERENCES "workflow_template_stages" ("id") ON UPDATE NO ACTION ON DELETE NO ACTION;

-- 5. 기존 컬럼/인덱스 제거
-- changeset moonseongha:1775394647437-18 splitStatements:false
DROP INDEX "ix_engineering_change_steps_step_type";

-- changeset moonseongha:1775394647437-16 splitStatements:false
ALTER TABLE "engineering_change_steps" DROP COLUMN "sequence";

-- changeset moonseongha:1775394647437-17 splitStatements:false
ALTER TABLE "engineering_change_steps" DROP COLUMN "step_type";
