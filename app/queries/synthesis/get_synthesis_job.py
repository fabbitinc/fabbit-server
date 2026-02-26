"""합성 작업 단건 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.synthesis import repository as repo
from app.modules.synthesis.schemas import SynthesisJobResponse
from app.modules.synthesis.mapper import to_job_response


@transactional(read_only=True)
def get_synthesis_job(db: Session, job_id: uuid.UUID) -> SynthesisJobResponse:
    job = repo.get_synthesis_job_by_id(db, job_id)
    if job is None:
        raise AppError(message="합성 작업을 찾을 수 없습니다", code="NOT_FOUND")
    return to_job_response(job)
