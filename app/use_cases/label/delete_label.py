"""라벨 삭제."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.label import service as label_service


@transactional()
def delete_label(
    db: Session,
    auth: AuthContext,
    label_id: uuid.UUID,
) -> None:
    """라벨 삭제."""
    label_service.delete_label(db, label_id)
