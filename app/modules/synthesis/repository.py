"""합성 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy import text
from sqlalchemy.orm import Session

from app.modules.mapping.models import MappingRecord
from app.modules.project.models import Project
from app.modules.synthesis.models import SynthesisBatch, SynthesisJob
from app.modules.upload.models import Upload


def set_search_path(db: Session, schema_name: str) -> None:
    db.execute(text(f"SET search_path = {schema_name}, ag_catalog, public"))


def get_mapping_by_id(db: Session, mapping_id: uuid.UUID) -> MappingRecord | None:
    return db.query(MappingRecord).filter(MappingRecord.id == mapping_id).first()


def get_latest_mapping(db: Session) -> MappingRecord | None:
    return db.query(MappingRecord).order_by(MappingRecord.created_at.desc()).first()


def get_latest_mapping_by_project(
    db: Session,
    project_id: uuid.UUID,
) -> MappingRecord | None:
    return (
        db.query(MappingRecord)
        .join(Upload, Upload.id == MappingRecord.upload_id)
        .filter(Upload.project_id == project_id)
        .order_by(MappingRecord.created_at.desc())
        .first()
    )


def get_project_by_id(db: Session, project_id: uuid.UUID) -> Project | None:
    return db.query(Project).filter(Project.id == project_id).first()


def get_upload_by_id(db: Session, upload_id: uuid.UUID) -> Upload | None:
    return db.query(Upload).filter(Upload.id == upload_id).first()


def create_synthesis_job(
    db: Session,
    job_id: uuid.UUID,
    mapping_id: uuid.UUID,
    upload_id: uuid.UUID,
    batch_id: uuid.UUID | None = None,
) -> SynthesisJob:
    job = SynthesisJob(
        id=job_id,
        batch_id=batch_id,
        mapping_id=mapping_id,
        upload_id=upload_id,
        status="PENDING",
    )
    db.add(job)
    return job


def increment_mapping_usage(record: MappingRecord, amount: int = 1) -> None:
    record.usage_count += amount


def create_synthesis_batch(
    db: Session,
    batch_id: uuid.UUID,
    project_id: uuid.UUID,
    mapping_id: uuid.UUID,
    requested_count: int,
    accepted_count: int,
    failed_uploads: list[dict],
) -> SynthesisBatch:
    batch = SynthesisBatch(
        id=batch_id,
        project_id=project_id,
        mapping_id=mapping_id,
        requested_count=requested_count,
        accepted_count=accepted_count,
        failed_uploads=failed_uploads,
    )
    db.add(batch)
    return batch


def get_synthesis_job_by_id(db: Session, job_id: uuid.UUID) -> SynthesisJob | None:
    return db.query(SynthesisJob).filter(SynthesisJob.id == job_id).first()


def get_synthesis_job_required(db: Session, job_id: uuid.UUID) -> SynthesisJob:
    return db.query(SynthesisJob).filter(SynthesisJob.id == job_id).one()


def get_synthesis_batch_by_id(
    db: Session, batch_id: uuid.UUID
) -> SynthesisBatch | None:
    return db.query(SynthesisBatch).filter(SynthesisBatch.id == batch_id).first()


def list_synthesis_jobs_by_batch_id(
    db: Session,
    batch_id: uuid.UUID,
) -> list[SynthesisJob]:
    return (
        db.query(SynthesisJob)
        .filter(SynthesisJob.batch_id == batch_id)
        .order_by(SynthesisJob.created_at.asc())
        .all()
    )


def list_synthesis_jobs(db: Session) -> list[SynthesisJob]:
    return db.query(SynthesisJob).order_by(SynthesisJob.created_at.desc()).all()
