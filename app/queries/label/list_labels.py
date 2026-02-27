"""프로젝트 라벨 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import mapper, repository as repo
from app.modules.label.schemas import LabelListResponse


@transactional(read_only=True)
def list_labels(
    db: Session,
    auth: AuthContext,
    project_id: uuid.UUID,
) -> LabelListResponse:
    """프로젝트의 전체 라벨 목록 조회."""
    labels = repo.list_by_project(db, project_id)
    items = [mapper.to_label_response(label) for label in labels]
    return LabelListResponse(total=len(items), items=items)
