"""매핑 도메인 서비스 레이어."""

import time
import uuid
from datetime import datetime, timezone

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.excel_parser import (
    extract_headers_and_rows,
    get_sheet_names,
)
from app.infrastructure.s3_client import S3Client
from app.modules.ai_usage.service import log_ai_usage
from app.modules.mapping import repository as repo
from app.modules.mapping.constants import MappingScope
from app.modules.mapping.models import MappingRecord, MappingRevision
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
    MappingImpactSummary,
    MappingUpdateRequest,
    MappingValidateRequest,
    MappingValidateResponse,
    SheetPreview,
    SkippedSheet,
    ValidationIssue,
)
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import MappingResult
from app.modules.ontology import service as ontology_service

_s3 = S3Client()

# merge key 캐시: {label: {merge_key_name, ...}}
_MERGE_KEYS_BY_LABEL = {
    nl.label: set(nl.merge_keys) for nl in MANUFACTURING_ONTOLOGY.node_labels
}


def _determine_scope(mapping: MappingResult) -> MappingScope:
    """매핑 내용 기반 scope 자동 판별."""
    if not mapping.relation_mappings:
        return MappingScope.PART_LIST

    for rm in mapping.relation_mappings:
        required_keys = _MERGE_KEYS_BY_LABEL.get(rm.target_label, set())
        for merge_key in required_keys:
            if not rm.node_columns.get(merge_key):
                return MappingScope.ROOT_BOM

    return MappingScope.FULL_BOM


@transactional(read_only=True)
def preview_mapping(
    db: Session,
    auth: AuthContext,
    req: MappingPreviewRequest,
) -> MappingPreviewResponse:
    t_total = time.perf_counter()
    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if file.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    content = _s3.get_object(file.file_key)

    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheets = [req.sheet_name]
    elif is_excel:
        target_sheets = sheet_names
    else:
        target_sheets = [None]

    sheets: list[SheetPreview] = []
    skipped_sheets: list[SkippedSheet] = []
    first_headers: list[str] = []
    first_sample_rows: list[dict] = []
    first_mapping = None

    for sheet in target_sheets:
        sheet_label = sheet or file.original_name
        try:
            t_parse = time.perf_counter()
            headers, sample_rows = extract_headers_and_rows(
                content,
                file.original_name,
                sheet_name=sheet,
                max_rows=5,
            )
            logger.info(
                "[매핑] 파싱 완료: {sheet} ({elapsed:.2f}s)",
                sheet=sheet_label,
                elapsed=time.perf_counter() - t_parse,
            )
        except Exception as e:
            if sheet is not None:
                skipped_sheets.append(
                    SkippedSheet(
                        sheet_name=sheet,
                        reason=f"파싱 실패: {e}",
                    )
                )
            continue

        if not headers:
            if sheet is not None:
                skipped_sheets.append(
                    SkippedSheet(
                        sheet_name=sheet,
                        reason="헤더를 추출할 수 없습니다",
                    )
                )
            continue

        t_llm = time.perf_counter()
        mapping_result, llm_resp = ontology_service.generate_mapping(
            headers, sample_rows
        )
        logger.info(
            "[매핑] LLM 매핑 완료: {sheet} ({elapsed:.1f}s)",
            sheet=sheet_label,
            elapsed=time.perf_counter() - t_llm,
        )

        log_ai_usage(
            org_id=auth.org_id,
            user_id=auth.account_id,
            feature="mapping:preview",
            model=llm_resp.model,
            input_tokens=llm_resp.input_tokens,
            output_tokens=llm_resp.output_tokens,
        )

        if not mapping_result.property_mappings:
            if sheet is not None:
                skipped_sheets.append(
                    SkippedSheet(
                        sheet_name=sheet,
                        reason="온톨로지에 매핑 가능한 컬럼이 없습니다",
                    )
                )
            continue

        if sheet is not None:
            sheets.append(
                SheetPreview(
                    sheet_name=sheet,
                    headers=headers,
                    sample_rows=sample_rows,
                    mapping=mapping_result,
                )
            )

        if first_mapping is None:
            first_headers = headers
            first_sample_rows = sample_rows
            first_mapping = mapping_result

    if first_mapping is None:
        raise AppError(
            message="파일에서 매핑 가능한 데이터를 찾을 수 없습니다",
            code="INVALID_INPUT",
        )

    total_elapsed = time.perf_counter() - t_total
    logger.info(
        "매핑 미리보기 완료: file_id={file_id} sheets={sheet_count}개 skipped={skipped_count}개 총 {elapsed:.1f}s",
        file_id=req.file_id,
        sheet_count=len(sheets),
        skipped_count=len(skipped_sheets),
        elapsed=total_elapsed,
    )
    return MappingPreviewResponse(
        headers=first_headers,
        sample_rows=first_sample_rows,
        mapping=first_mapping,
        sheets=sheets,
        skipped_sheets=skipped_sheets,
    )


@transactional(read_only=True)
def validate_mapping(
    db: Session,
    req: MappingValidateRequest,
) -> MappingValidateResponse:
    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(file.file_key)
    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheet = req.sheet_name
    elif is_excel:
        target_sheet = sheet_names[0]
    else:
        target_sheet = None

    headers, sample_rows = extract_headers_and_rows(
        content,
        file.original_name,
        sheet_name=target_sheet,
        max_rows=30,
    )
    header_set = set(headers)

    normalized_mapping = ontology_service.normalize_mapping(req.mapping)
    errors: list[ValidationIssue] = []
    warnings: list[ValidationIssue] = []

    merge_keys_by_label = {
        nl.label: set(nl.merge_keys) for nl in MANUFACTURING_ONTOLOGY.node_labels
    }

    # Part 속성 매핑 검증
    for idx, pm in enumerate(normalized_mapping.property_mappings):
        if pm.source_column not in header_set:
            errors.append(
                ValidationIssue(
                    code="MISSING_SOURCE_COLUMN",
                    severity="error",
                    message=f"컬럼 '{pm.source_column}'을(를) 파일에서 찾을 수 없습니다",
                    path=f"property_mappings[{idx}].source_column",
                    dismissed_reason="missing_source_column",
                )
            )
            continue
        if pm.data_type in ("integer", "float") and _has_non_numeric_sample(
            sample_rows,
            pm.source_column,
        ):
            warnings.append(
                ValidationIssue(
                    code="NUMERIC_PARSE_WARNING",
                    severity="warning",
                    message=(
                        f"컬럼 '{pm.source_column}'에 숫자로 해석하기 어려운 값이 있습니다"
                    ),
                    path=f"property_mappings[{idx}].data_type",
                )
            )

    # 관계 매핑 검증
    rel_defs = {rt.rel_type: rt for rt in MANUFACTURING_ONTOLOGY.relationship_types}

    for idx, rm in enumerate(normalized_mapping.relation_mappings):
        rel_def = rel_defs.get(rm.rel_type)
        if rel_def is None:
            errors.append(
                ValidationIssue(
                    code="INVALID_REL_TYPE",
                    severity="error",
                    message=f"허용되지 않은 관계 타입입니다: {rm.rel_type}",
                    path=f"relation_mappings[{idx}].rel_type",
                )
            )
            continue

        # Rootless relation: node_columns 없는 관계는 merge key 검증 스킵
        is_rootless = not rm.node_columns and rm.rel_columns

        # node_columns 검증: 상대방 노드의 merge key가 매핑되어 있는지
        if is_rootless:
            pass  # rootless relation은 합성 시점에 root_context로 보정
        else:
            required_keys = merge_keys_by_label.get(rm.target_label, set())
            for merge_key in required_keys:
                src_col = rm.node_columns.get(merge_key)
                if not src_col:
                    errors.append(
                        ValidationIssue(
                            code="MISSING_NODE_MERGE_KEY",
                            severity="error",
                            message=(
                                f"관계 '{rm.rel_type}'의 대상 노드 merge key "
                                f"'{merge_key}' 매핑이 누락되었습니다"
                            ),
                            path=f"relation_mappings[{idx}].node_columns.{merge_key}",
                            dismissed_reason="missing_node_merge_key",
                        )
                    )
                    continue
                if src_col not in header_set:
                    errors.append(
                        ValidationIssue(
                            code="MISSING_SOURCE_COLUMN",
                            severity="error",
                            message=f"컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다",
                            path=f"relation_mappings[{idx}].node_columns.{merge_key}",
                            dismissed_reason="missing_source_column",
                        )
                    )

        # rel_columns 검증
        for rel_prop, src_col in rm.rel_columns.items():
            path = f"relation_mappings[{idx}].rel_columns.{rel_prop}"
            if src_col not in header_set:
                errors.append(
                    ValidationIssue(
                        code="MISSING_SOURCE_COLUMN",
                        severity="error",
                        message=(
                            f"관계 속성 컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다"
                        ),
                        path=path,
                        dismissed_reason="missing_source_column",
                    )
                )
                continue

            data_type = rm.rel_column_types.get(rel_prop, "string")
            if data_type in ("integer", "float") and _has_non_numeric_sample(
                sample_rows,
                src_col,
            ):
                warnings.append(
                    ValidationIssue(
                        code="NUMERIC_PARSE_WARNING",
                        severity="warning",
                        message=(
                            f"관계 속성 컬럼 '{src_col}'에 숫자로 해석하기 어려운 값이 있습니다"
                        ),
                        path=f"relation_mappings[{idx}].rel_column_types.{rel_prop}",
                    )
                )

    # 미사용 컬럼 카운트
    used_columns = {pm.source_column for pm in normalized_mapping.property_mappings}
    for rm in normalized_mapping.relation_mappings:
        used_columns.update(rm.node_columns.values())
        used_columns.update(rm.rel_columns.values())

    impact_summary = MappingImpactSummary(
        disabled_column_count=sum(1 for h in headers if h not in used_columns),
    )

    return MappingValidateResponse(
        normalized_mapping=normalized_mapping,
        errors=errors,
        warnings=warnings,
        impact_summary=impact_summary,
    )


@transactional
def confirm_mapping(
    db: Session,
    req: MappingConfirmRequest,
) -> MappingResponse:
    validation = validate_mapping(
        db,
        MappingValidateRequest(
            file_id=req.file_id,
            sheet_name=req.sheet_name,
            mapping=req.mapping,
        ),
    )
    if validation.errors:
        detail = "; ".join(issue.message for issue in validation.errors[:3])
        raise AppError(
            message=f"매핑 검증에 실패했습니다: {detail}",
            code="INVALID_MAPPING",
        )

    # 이름 중복 검사
    if repo.exists_by_name(db, req.name):
        raise AppError(
            message=f"이미 동일한 이름의 매핑이 존재합니다: '{req.name}'",
            code="DUPLICATE_NAME",
        )

    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(file.file_key)

    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheet = req.sheet_name
    elif is_excel:
        target_sheet = sheet_names[0]
    else:
        target_sheet = None

    headers, _ = extract_headers_and_rows(
        content,
        file.original_name,
        sheet_name=target_sheet,
        max_rows=0,
    )

    scope = _determine_scope(validation.normalized_mapping)

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
        mapping=validation.normalized_mapping.model_dump(),
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
    return _to_mapping_response(record, revision)


@transactional(read_only=True)
def list_mappings(db: Session) -> MappingListResponse:
    pairs = repo.list_mappings(db)
    return MappingListResponse(
        items=[_to_mapping_response(r, rev) for r, rev in pairs]
    )


@transactional(read_only=True)
def get_mapping(db: Session, mapping_id: uuid.UUID) -> MappingResponse:
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, revision = result
    return _to_mapping_response(record, revision)


@transactional
def update_mapping(
    db: Session,
    mapping_id: uuid.UUID,
    req: MappingUpdateRequest,
) -> MappingResponse:
    """새 리비전 생성 (name 변경 시 Record도 업데이트)."""
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, latest_rev = result

    if not record.is_active:
        raise AppError(message="비활성화된 매핑은 수정할 수 없습니다", code="PRECONDITION_FAILED")

    # 매핑 검증
    validation = validate_mapping(
        db,
        MappingValidateRequest(
            file_id=req.file_id,
            sheet_name=req.sheet_name,
            mapping=req.mapping,
        ),
    )
    if validation.errors:
        detail = "; ".join(issue.message for issue in validation.errors[:3])
        raise AppError(
            message=f"매핑 검증에 실패했습니다: {detail}",
            code="INVALID_MAPPING",
        )

    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(file.file_key)
    sheet_names = get_sheet_names(content, file.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheet = req.sheet_name
    elif is_excel:
        target_sheet = sheet_names[0]
    else:
        target_sheet = None

    headers, _ = extract_headers_and_rows(
        content,
        file.original_name,
        sheet_name=target_sheet,
        max_rows=0,
    )

    # name 변경 시 중복 검사 후 Record 업데이트
    if req.name is not None and req.name != record.name:
        if repo.exists_by_name(db, req.name, exclude_id=record.id):
            raise AppError(
                message=f"이미 동일한 이름의 매핑이 존재합니다: '{req.name}'",
                code="DUPLICATE_NAME",
            )
        record.name = req.name

    # 새 매핑 내용에 따라 scope 재판별
    record.scope = _determine_scope(validation.normalized_mapping)
    record.updated_at = datetime.now(timezone.utc)

    new_revision = MappingRevision(
        id=generate_uuid7(),
        record_id=record.id,
        file_id=req.file_id,
        version=latest_rev.version + 1,
        sheet_name=req.sheet_name,
        original_headers=headers,
        mapping=validation.normalized_mapping.model_dump(),
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
    return _to_mapping_response(record, new_revision)


@transactional
def deactivate_mapping(db: Session, mapping_id: uuid.UUID) -> None:
    """매핑 비활성화 (soft-delete)."""
    result = repo.get_mapping_by_id(db, mapping_id)
    if result is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    record, _ = result
    record.is_active = False
    logger.info("매핑 비활성화: mapping_id={mapping_id}", mapping_id=record.id)


def _to_mapping_response(
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
            key=lambda col: original_headers.index(col) if col in original_headers else len(original_headers),
        ),
        mapping=mapping_payload,
        scope=record.scope,
        is_active=record.is_active,
        usage_count=record.usage_count,
        version=revision.version,
        created_at=record.created_at,
    )


def _has_non_numeric_sample(sample_rows: list[dict], source_column: str) -> bool:
    values = [row.get(source_column) for row in sample_rows]
    for value in values:
        if value is None:
            continue
        text = str(value).strip()
        if not text:
            continue
        if _can_parse_numeric(text):
            continue
        return True
    return False


def _can_parse_numeric(value: str) -> bool:
    cleaned = value.replace(",", "").strip()
    try:
        float(cleaned)
        return True
    except ValueError:
        return False
