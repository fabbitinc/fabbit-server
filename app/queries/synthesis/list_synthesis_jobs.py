"""합성 작업 목록 조회."""

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.synthesis import repository as repo
from app.modules.synthesis.schemas import SynthesisListResponse
from app.modules.synthesis.mapper import to_job_response


@transactional(read_only=True)
def list_synthesis_jobs(db: Session) -> SynthesisListResponse:
    jobs = repo.list_synthesis_jobs(db)
    return SynthesisListResponse(items=[to_job_response(j) for j in jobs])
