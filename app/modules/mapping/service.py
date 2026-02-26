import time
import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.excel_parser import extract_headers_and_rows, get_sheet_names
from app.infrastructure.s3_client import s3_client
from app.modules.file.models import File
from app.modules.mapping import repository as repo
from app.modules.mapping.constants import MappingScope
from app.modules.mapping.mapper import to_mapping_response
from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingImpactSummary,
    MappingResponse,
    MappingUpdateRequest,
    ValidationIssue,
)
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import MappingResult

_s3 = s3_client
_MERGE_KEYS_BY_LABEL = {
    nl.label: set(nl.merge_keys) for nl in MANUFACTURING_ONTOLOGY.node_labels
}


def _determine_scope(mapping: MappingResult) -> MappingScope:
    if not mapping.relation_mappings:
        return MappingScope.PART_LIST

    for rel_mapping in mapping.relation_mappings:
        required_keys = _MERGE_KEYS_BY_LABEL.get(rel_mapping.target_label, set())
        for merge_key in required_keys:
            if not rel_mapping.node_columns.get(merge_key):
                return MappingScope.ROOT_BOM

    return MappingScope.FULL_BOM


def get_uploaded_file_or_raise(db: Session, file_id: uuid.UUID) -> File:
    file = repo.get_file_by_id(db, file_id)
    if file is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if file.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )
    return file


def resolve_target_sheet(file: File, sheet_name: str | None) -> str | None:
    content = _s3.get_object(file.file_key)
    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0
    if sheet_name is not None:
        return sheet_name
    if is_excel:
        return sheet_names[0]
    return None


def load_headers_and_rows(
    file: File,
    *,
    sheet_name: str | None,
    max_rows: int,
) -> tuple[list[str], list[dict[str, object]]]:
    content = _s3.get_object(file.file_key)
    target_sheet = resolve_target_sheet(file, sheet_name)
    return extract_headers_and_rows(
        content,
        file.original_name,
        sheet_name=target_sheet,
        max_rows=max_rows,
    )


def create_mapping(
    db: Session,
    req: MappingConfirmRequest,
    normalized_mapping: MappingResult,
) -> MappingResponse:
    if repo.exists_by_name(db, req.name):
        raise AppError(
            message=f"이미 동일한 이름의 매핑이 존재합니다: '{req.name}'",
            code="DUPLICATE_NAME",
        )

    file = get_uploaded_file_or_raise(db, req.file_id)
    headers, _ = load_headers_and_rows(file, sheet_name=req.sheet_name, max_rows=0)
    scope = _determine_scope(normalized_mapping)

    record = MappingRecord(
        id=generate_uuid7(),
        name=req.name,
        scope=scope,
        usage_count=0,
    )
    revision = MappingRevision(
        id=generate_uuid7(),
        record_id=record.id,
        file_id=req.file_id,
        version=1,
        sheet_name=req.sheet_name,
        original_headers=headers,
        mapping=normalized_mapping.model_dump(),
        usage_count=0,
    )
    repo.create_mapping_record(db, record, revision)
    db.flush()
    db.refresh(record)
    db.refresh(revision)

    logger.info(
        "매핑 확정: mapping_id={mapping_id} name={name} scope={scope}",
        mapping_id=record.id,
        name=record.name,
        scope=record.scope,
    )
    return to_mapping_response(record, revision)


def update_mapping(
    db: Session,
    mapping_id: uuid.UUID,
    req: MappingUpdateRequest,
    normalized_mapping: MappingResult,
) -> MappingResponse:
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, latest_rev = result

    if not record.is_active:
        raise AppError(
            message="비활성화된 매핑은 수정할 수 없습니다", code="PRECONDITION_FAILED"
        )

    file = get_uploaded_file_or_raise(db, req.file_id)
    headers, _ = load_headers_and_rows(file, sheet_name=req.sheet_name, max_rows=0)

    if req.name is not None and req.name != record.name:
        if repo.exists_by_name(db, req.name, exclude_id=record.id):
            raise AppError(
                message=f"이미 동일한 이름의 매핑이 존재합니다: '{req.name}'",
                code="DUPLICATE_NAME",
            )
        record.rename(req.name)

    record.update_scope(_determine_scope(normalized_mapping))
    new_revision = MappingRevision(
        id=generate_uuid7(),
        record_id=record.id,
        file_id=req.file_id,
        version=latest_rev.version + 1,
        sheet_name=req.sheet_name,
        original_headers=headers,
        mapping=normalized_mapping.model_dump(),
        usage_count=0,
    )
    repo.create_revision(db, new_revision)
    db.flush()
    db.refresh(record)
    db.refresh(new_revision)

    logger.info(
        "매핑 업데이트: mapping_id={mapping_id} version={version}",
        mapping_id=record.id,
        version=new_revision.version,
    )
    return to_mapping_response(record, new_revision)


def deactivate_mapping(db: Session, mapping_id: uuid.UUID) -> None:
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, _ = result
    record.deactivate()
    logger.info("매핑 비활성화: mapping_id={mapping_id}", mapping_id=record.id)


def load_preview_targets(
    file: File, req_sheet_name: str | None
) -> tuple[str | None, ...]:
    content = _s3.get_object(file.file_key)
    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0
    if req_sheet_name is not None:
        return (req_sheet_name,)
    if is_excel:
        return tuple(sheet_names)
    return (None,)


def validate_against_rows(
    headers: list[str],
    sample_rows: list[dict[str, object]],
    mapping: MappingResult,
) -> tuple[list[ValidationIssue], list[ValidationIssue], MappingImpactSummary]:
    from app.modules.mapping.validation import validate_mapping_against_rows

    return validate_mapping_against_rows(headers, sample_rows, mapping)


def parse_sheet_preview(
    file: File,
    *,
    sheet_name: str | None,
    max_rows: int,
) -> tuple[list[str], list[dict[str, object]]]:
    content = _s3.get_object(file.file_key)
    t_parse = time.perf_counter()
    headers, sample_rows = extract_headers_and_rows(
        content,
        file.original_name,
        sheet_name=sheet_name,
        max_rows=max_rows,
    )
    logger.info(
        "[매핑] 파싱 완료: {sheet} ({elapsed:.2f}s)",
        sheet=sheet_name or file.original_name,
        elapsed=time.perf_counter() - t_parse,
    )
    return headers, sample_rows
