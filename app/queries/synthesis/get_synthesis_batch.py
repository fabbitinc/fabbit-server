"""합성 배치 상태 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.synthesis import repository as repo
from app.modules.synthesis.schemas import (
    SynthesisBatchFailure,
    SynthesisBatchItemStatus,
    SynthesisBatchStatusResponse,
)


@transactional(read_only=True)
def get_synthesis_batch(
    db: Session,
    batch_id: uuid.UUID,
) -> SynthesisBatchStatusResponse:
    batch = repo.get_synthesis_batch_by_id(db, batch_id)
    if batch is None:
        raise AppError(message="합성 배치를 찾을 수 없습니다", code="NOT_FOUND")

    jobs = repo.list_synthesis_jobs_by_batch_id(db, batch_id)
    pending_count = 0
    processing_count = 0
    completed_count = 0
    failed_job_count = 0
    items = []

    for job in jobs:
        if job.status == "PENDING":
            pending_count += 1
        elif job.status == "PROCESSING":
            processing_count += 1
        elif job.status == "FAILED":
            failed_job_count += 1
        elif job.status == "COMPLETED":
            completed_count += 1

        items.append(
            SynthesisBatchItemStatus(
                job_id=job.id,
                file_id=job.file_id,
                status=job.status,
                total_rows=job.total_rows,
                processed_rows=job.processed_rows,
                nodes_created=job.nodes_created,
                relationships_created=job.relationships_created,
                error_count=len(job.errors or []),
                started_at=job.started_at,
                completed_at=job.completed_at,
            )
        )

    failed = [
        SynthesisBatchFailure.model_validate(item)
        for item in batch.failed_uploads or []
    ]
    failed_count = len(failed)
    accepted_count = batch.accepted_count
    done_count = completed_count + failed_job_count

    if accepted_count == 0:
        status = "FAILED" if failed_count > 0 else "PENDING"
    elif done_count == accepted_count:
        status = "COMPLETED" if failed_job_count == 0 else "COMPLETED_WITH_ERRORS"
    elif processing_count > 0:
        status = "PROCESSING"
    else:
        status = "PENDING"

    return SynthesisBatchStatusResponse(
        batch_id=batch.id,
        requested_count=batch.requested_count,
        accepted_count=accepted_count,
        failed_count=failed_count,
        pending_count=pending_count,
        processing_count=processing_count,
        completed_count=completed_count,
        failed_job_count=failed_job_count,
        status=status,
        failed=failed,
        items=items,
        created_at=batch.created_at,
    )
