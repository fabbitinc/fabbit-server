"""합성 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.modules.mapping.models import MappingRecord
from app.modules.synthesis.models import SynthesisJob
from app.modules.upload.models import Upload


def set_search_path(db: Session, schema_name: str) -> None:
    db.execute(text(f"SET search_path = {schema_name}, ag_catalog, public"))


def get_mapping_by_id(db: Session, mapping_id: uuid.UUID) -> MappingRecord | None:
    return db.query(MappingRecord).filter(MappingRecord.id == mapping_id).first()


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


def create_synthesis_job(
    db: Session,
    job_id: uuid.UUID,
    mapping_id: uuid.UUID,
    upload_id: uuid.UUID,
) -> SynthesisJob:
    job = SynthesisJob(
        id=job_id,
        mapping_id=mapping_id,
        upload_id=upload_id,
        status="PENDING",
    )
    db.add(job)
    return job


def increment_mapping_usage(record: MappingRecord) -> None:
    record.usage_count += 1


def get_synthesis_job_by_id(db: Session, job_id: uuid.UUID) -> SynthesisJob | None:
    return db.query(SynthesisJob).filter(SynthesisJob.id == job_id).first()


def get_synthesis_job_required(db: Session, job_id: uuid.UUID) -> SynthesisJob:
    return db.query(SynthesisJob).filter(SynthesisJob.id == job_id).one()


def list_synthesis_jobs(db: Session) -> list[SynthesisJob]:
    return db.query(SynthesisJob).order_by(SynthesisJob.created_at.desc()).all()
