"""합성 배치 시작."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.synthesis import service as synthesis_service
from app.modules.synthesis.schemas import (
    SynthesisBatchStartResponse,
    SynthesisStartRequest,
)


@transactional()
def start_synthesis(
    db: Session,
    auth: AuthContext,
    req: SynthesisStartRequest,
    add_background_task,
) -> SynthesisBatchStartResponse:
    return synthesis_service.start_synthesis(db, auth, req, add_background_task)
