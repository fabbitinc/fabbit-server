from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.mapping import repository as repo
from app.modules.mapping.mapper import to_mapping_response
from app.modules.mapping.schemas import MappingListResponse


@transactional(read_only=True)
def list_mappings(db: Session) -> MappingListResponse:
    pairs = repo.list_mappings(db)
    return MappingListResponse(items=[to_mapping_response(r, rev) for r, rev in pairs])
