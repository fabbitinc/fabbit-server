-- V2 테이블을 canonical 이름으로 변경
ALTER TABLE IF EXISTS mapping_v2_records RENAME TO mapping_records;
ALTER TABLE IF EXISTS mapping_v2_revisions RENAME TO mapping_revisions;
ALTER TABLE IF EXISTS synthesis_v2_batches RENAME TO synthesis_batches;
ALTER TABLE IF EXISTS synthesis_v2_jobs RENAME TO synthesis_jobs;

-- 인덱스 이름도 변경
ALTER INDEX IF EXISTS ix_mapping_v2_revisions_record_id RENAME TO ix_mapping_revisions_record_id;
ALTER INDEX IF EXISTS ix_synthesis_v2_jobs_batch_id RENAME TO ix_synthesis_jobs_batch_id;
ALTER INDEX IF EXISTS ix_synthesis_v2_jobs_mapping_id RENAME TO ix_synthesis_jobs_mapping_id;
ALTER INDEX IF EXISTS ix_synthesis_v2_jobs_file_id RENAME TO ix_synthesis_jobs_file_id;
