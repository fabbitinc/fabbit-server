"""라벨 생성."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import mapper, service as label_service
from app.modules.label.schemas import LabelResponse


@transactional()
def create_label(
    db: Session,
    auth: AuthContext,
    name: str,
    color: str,
    description: str | None = None,
) -> LabelResponse:
    """라벨 생성."""
    label = label_service.create_label(
        db, name=name, color=color, description=description
    )
    return mapper.to_label_response(label)
