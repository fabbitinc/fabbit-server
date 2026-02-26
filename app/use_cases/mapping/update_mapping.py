import uuid

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.mapping import service as mapping_service
from app.modules.mapping.schemas import MappingResponse, MappingUpdateRequest
from app.modules.mapping.validation import validate_mapping_against_rows
from app.modules.ontology import service as ontology_service


@transactional()
def update_mapping(
    db: Session,
    mapping_id: uuid.UUID,
    req: MappingUpdateRequest,
) -> MappingResponse:
    file = mapping_service.get_uploaded_file_or_raise(db, req.file_id)
    headers, sample_rows = mapping_service.load_headers_and_rows(
        file,
        sheet_name=req.sheet_name,
        max_rows=30,
    )
    normalized_mapping = ontology_service.normalize_mapping(req.mapping)
    errors, _, _ = validate_mapping_against_rows(
        headers, sample_rows, normalized_mapping
    )
    if errors:
        detail = "; ".join(issue.message for issue in errors[:3])
        raise AppError(
            message=f"매핑 검증에 실패했습니다: {detail}",
            code="INVALID_MAPPING",
        )

    return mapping_service.update_mapping(db, mapping_id, req, normalized_mapping)
