"""라벨 수정 — 프로젝트/라벨 존재 검증 후 수정."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import mapper, service as label_service
from app.modules.label.schemas import LabelResponse


@transactional()
def update_label(
    db: Session,
    auth: AuthContext,
    label_id: uuid.UUID,
    *,
    name: str | None = None,
    description: str | None = None,
    color: str | None = None,
    _unset_description: bool = False,
) -> LabelResponse:
    """라벨 수정."""
    label = label_service.update_label(
        db,
        label_id,
        name=name,
        description=description,
        color=color,
        _unset_description=_unset_description,
    )
    return mapper.to_label_response(label)
