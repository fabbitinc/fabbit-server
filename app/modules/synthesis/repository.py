"""합성 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher_raw
from app.modules.file.models import File
from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.synthesis.models import SynthesisBatch, SynthesisJob


def get_mapping_by_id(db: Session, mapping_id: uuid.UUID) -> MappingRecord | None:
    return (
        db.query(MappingRecord)
        .filter(MappingRecord.id == mapping_id, MappingRecord.is_active.is_(True))
        .first()
    )


def get_latest_revision(db: Session, record_id: uuid.UUID) -> MappingRevision | None:
    return (
        db.query(MappingRevision)
        .filter(MappingRevision.record_id == record_id)
        .order_by(MappingRevision.version.desc())
        .first()
    )


def get_file_by_id(db: Session, file_id: uuid.UUID) -> File | None:
    return db.query(File).filter(File.id == file_id).first()


def create_synthesis_job(
    db: Session,
    mapping_id: uuid.UUID,
    file_id: uuid.UUID,
    batch_id: uuid.UUID | None = None,
) -> SynthesisJob:
    job = SynthesisJob.create(
        mapping_id=mapping_id,
        file_id=file_id,
        batch_id=batch_id,
    )
    db.add(job)
    return job


def increment_mapping_usage(
    db: Session,
    record: MappingRecord,
    revision: MappingRevision,
    amount: int = 1,
) -> None:
    record.increment_usage(amount)
    revision.usage_count += amount


def create_synthesis_batch(
    db: Session,
    batch_id: uuid.UUID,
    project_id: uuid.UUID | None,
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


# ── AGE 그래프 쓰기 ──


def execute_graph_cyphers(
    db: Session,
    graph_name: str,
    cyphers: list[str],
) -> int:
    """Cypher 쿼리 목록을 순차 실행. 실행된 수를 반환."""
    count = 0
    for cypher in cyphers:
        execute_cypher_raw(db, cypher, graph_name)
        count += 1
    return count
