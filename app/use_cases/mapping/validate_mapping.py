from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.mapping import service as mapping_service
from app.modules.mapping.schemas import MappingValidateRequest, MappingValidateResponse
from app.modules.ontology import service as ontology_service


@transactional(read_only=True)
def validate_mapping(
    db: Session,
    req: MappingValidateRequest,
) -> MappingValidateResponse:
    file = mapping_service.get_uploaded_file_or_raise(db, req.file_id)
    headers, sample_rows = mapping_service.load_headers_and_rows(
        file,
        sheet_name=req.sheet_name,
        max_rows=30,
    )

    normalized_mapping = ontology_service.normalize_mapping(req.mapping)
    errors, warnings, impact_summary = mapping_service.validate_against_rows(
        headers,
        sample_rows,
        normalized_mapping,
    )

    return MappingValidateResponse(
        normalized_mapping=normalized_mapping,
        errors=errors,
        warnings=warnings,
        impact_summary=impact_summary,
    )
