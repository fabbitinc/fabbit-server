"""합성 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher_raw
from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.project.models import Project
from app.modules.synthesis.models import SynthesisBatch, SynthesisJob
from app.modules.upload.models import Upload


def get_mapping_by_id(db: Session, mapping_id: uuid.UUID) -> MappingRecord | None:
    return (
        db.query(MappingRecord)
        .filter(MappingRecord.id == mapping_id, MappingRecord.is_active.is_(True))
        .first()
    )


def get_latest_mapping(db: Session) -> MappingRecord | None:
    return (
        db.query(MappingRecord)
        .filter(MappingRecord.is_active.is_(True))
        .order_by(MappingRecord.created_at.desc())
        .first()
    )


def get_latest_mapping_by_project(
    db: Session,
    project_id: uuid.UUID,
) -> MappingRecord | None:
    """프로젝트에 속한 업로드를 참조하는 최신 활성 매핑 조회.

    MappingRevision → Upload 경유로 프로젝트 소속 여부를 판단합니다.
    """
    return (
        db.query(MappingRecord)
        .join(MappingRevision, MappingRevision.record_id == MappingRecord.id)
        .join(Upload, Upload.id == MappingRevision.upload_id)
        .filter(
            Upload.owner_type == "project",
            Upload.owner_id == project_id,
            MappingRecord.is_active.is_(True),
        )
        .order_by(MappingRecord.created_at.desc())
        .first()
    )


def get_latest_revision(db: Session, record_id: uuid.UUID) -> MappingRevision | None:
    return (
        db.query(MappingRevision)
        .filter(MappingRevision.record_id == record_id)
        .order_by(MappingRevision.version.desc())
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


def increment_mapping_usage(
    db: Session,
    record: MappingRecord,
    revision: MappingRevision,
    amount: int = 1,
) -> None:
    record.usage_count += amount
    revision.usage_count += amount


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
