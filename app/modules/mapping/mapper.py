from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.mapping.schemas import MappingResponse
from app.modules.ontology.schemas import MappingResult


def to_mapping_response(
    record: MappingRecord, revision: MappingRevision
) -> MappingResponse:
    original_headers: list[str] = []
    if isinstance(revision.original_headers, list):
        original_headers = [str(header) for header in revision.original_headers]

    mapping_payload = MappingResult(
        property_mappings=[],
        relation_mappings=[],
    )
    if isinstance(revision.mapping, dict):
        mapping_payload = MappingResult.model_validate(revision.mapping)

    return MappingResponse(
        id=record.id,
        file_id=revision.file_id,
        name=record.name,
        sheet_name=revision.sheet_name,
        original_headers=original_headers,
        mapped_headers=sorted(
            mapping_payload.get_required_columns(),
            key=lambda col: (
                original_headers.index(col)
                if col in original_headers
                else len(original_headers)
            ),
        ),
        mapping=mapping_payload,
        scope=record.scope,
        is_active=record.is_active,
        usage_count=record.usage_count,
        version=revision.version,
        created_at=record.created_at,
    )
