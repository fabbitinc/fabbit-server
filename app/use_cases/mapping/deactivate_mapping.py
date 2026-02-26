import uuid

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.mapping import service as mapping_service


@transactional()
def deactivate_mapping(db: Session, mapping_id: uuid.UUID) -> None:
    mapping_service.deactivate_mapping(db, mapping_id)
