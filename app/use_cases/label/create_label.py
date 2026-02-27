"""라벨 생성 — 프로젝트 존재 검증 후 라벨 생성."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import mapper, service as label_service
from app.modules.label.schemas import LabelResponse
from app.modules.project import service as project_service


@transactional()
def create_label(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
    name: str,
    color: str,
    description: str | None = None,
) -> LabelResponse:
    """라벨 생성."""
    project_service.get_or_raise(db, project_id)
    label = label_service.create_label(
        db, project_id, name=name, color=color, description=description
    )
    return mapper.to_label_response(label)
