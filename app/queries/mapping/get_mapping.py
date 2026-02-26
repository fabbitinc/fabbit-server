import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.mapping import repository as repo
from app.modules.mapping.mapper import to_mapping_response
from app.modules.mapping.schemas import MappingResponse


@transactional(read_only=True)
def get_mapping(db: Session, mapping_id: uuid.UUID) -> MappingResponse:
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, revision = result
    return to_mapping_response(record, revision)
