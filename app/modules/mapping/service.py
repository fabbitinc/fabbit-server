"""매핑 도메인 서비스 레이어."""

import re
import time
import uuid

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
from app.modules.mapping.models import MappingRecord
from app.modules.mapping.schemas import (
    EditableConstraints,
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
    MappingImpactSummary,
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
_EXT_NAME_RE = re.compile(r"^_ext_[a-z0-9_]+$")


def preview_mapping(
    db: Session,
    auth: AuthContext,
    req: MappingPreviewRequest,
) -> MappingPreviewResponse:
    t_total = time.perf_counter()
    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if upload.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    content = _s3.get_object(upload.file_key)

    sheet_names = get_sheet_names(content, upload.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        # 특정 시트만 처리
        target_sheets = [req.sheet_name]
    elif is_excel:
        # Excel이고 sheet_name=None이면 모든 시트 처리
        target_sheets = sheet_names
    else:
        # CSV는 시트 개념 없음
        target_sheets = [None]

    sheets: list[SheetPreview] = []
    skipped_sheets: list[SkippedSheet] = []
    first_headers: list[str] = []
    first_sample_rows: list[dict] = []
    first_mapping = None

    for sheet in target_sheets:
        sheet_label = sheet or upload.original_name
        try:
            t_parse = time.perf_counter()
            headers, sample_rows = extract_headers_and_rows(
                content,
                upload.original_name,
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

        if not mapping_result.column_mappings:
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

        # 첫 번째 유효 시트를 기본 응답으로 사용
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
        "매핑 미리보기 완료: upload_id={upload_id} sheets={sheet_count}개 skipped={skipped_count}개 총 {elapsed:.1f}s",
        upload_id=req.upload_id,
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
        editable_constraints=_build_editable_constraints(),
    )


def validate_mapping(
    db: Session,
    req: MappingValidateRequest,
) -> MappingValidateResponse:
    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(upload.file_key)
    sheet_names = get_sheet_names(content, upload.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheet = req.sheet_name
    elif is_excel:
        target_sheet = sheet_names[0]
    else:
        target_sheet = None

    headers, sample_rows = extract_headers_and_rows(
        content,
        upload.original_name,
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
    rel_defs = {rt.rel_type: rt for rt in MANUFACTURING_ONTOLOGY.relationship_types}

    mapped_properties: dict[str, set[str]] = {}

    for idx, cm in enumerate(normalized_mapping.column_mappings):
        mapped_properties.setdefault(cm.target_label, set()).add(cm.target_property)
        if cm.source_column not in header_set:
            errors.append(
                ValidationIssue(
                    code="MISSING_SOURCE_COLUMN",
                    severity="error",
                    message=f"컬럼 '{cm.source_column}'을(를) 파일에서 찾을 수 없습니다",
                    path=f"column_mappings[{idx}].source_column",
                    dismissed_reason="missing_source_column",
                )
            )
            continue
        if cm.data_type in ("integer", "float") and _has_non_numeric_sample(
            sample_rows,
            cm.source_column,
        ):
            warnings.append(
                ValidationIssue(
                    code="NUMERIC_PARSE_WARNING",
                    severity="warning",
                    message=(
                        f"컬럼 '{cm.source_column}'에 숫자로 해석하기 어려운 값이 있습니다"
                    ),
                    path=f"column_mappings[{idx}].data_type",
                )
            )

    for idx, ep in enumerate(normalized_mapping.extended_properties):
        if ep.source_column not in header_set:
            errors.append(
                ValidationIssue(
                    code="MISSING_SOURCE_COLUMN",
                    severity="error",
                    message=f"컬럼 '{ep.source_column}'을(를) 파일에서 찾을 수 없습니다",
                    path=f"extended_properties[{idx}].source_column",
                    dismissed_reason="missing_source_column",
                )
            )
        if not _EXT_NAME_RE.match(ep.property_name):
            errors.append(
                ValidationIssue(
                    code="INVALID_EXT_PROPERTY_NAME",
                    severity="error",
                    message=(
                        f"확장 속성명 '{ep.property_name}'은 _ext_ 접두사 + snake_case여야 합니다"
                    ),
                    path=f"extended_properties[{idx}].property_name",
                    dismissed_reason="invalid_ext_property_name",
                )
            )
        if ep.data_type in ("integer", "float") and _has_non_numeric_sample(
            sample_rows,
            ep.source_column,
        ):
            warnings.append(
                ValidationIssue(
                    code="NUMERIC_PARSE_WARNING",
                    severity="warning",
                    message=(
                        f"확장 컬럼 '{ep.source_column}'에 숫자로 해석하기 어려운 값이 있습니다"
                    ),
                    path=f"extended_properties[{idx}].data_type",
                )
            )

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

        _validate_relation_endpoint(
            errors=errors,
            idx=idx,
            direction="from",
            label=rm.from_label,
            endpoint_columns=rm.from_columns,
            headers=header_set,
            merge_keys_by_label=merge_keys_by_label,
        )
        _validate_relation_endpoint(
            errors=errors,
            idx=idx,
            direction="to",
            label=rm.to_label,
            endpoint_columns=rm.to_columns,
            headers=header_set,
            merge_keys_by_label=merge_keys_by_label,
        )

        if rm.from_label == rm.to_label:
            overlap = set(rm.from_columns.values()) & set(rm.to_columns.values())
            if overlap:
                errors.append(
                    ValidationIssue(
                        code="SELF_LOOP_ENDPOINT_CONFLICT",
                        severity="error",
                        message=(
                            "self-loop 관계는 from/to 엔드포인트 컬럼을 동일하게 사용할 수 없습니다"
                        ),
                        path=f"relation_mappings[{idx}]",
                    )
                )

        required_rel_props = {prop.name for prop in rel_def.properties if prop.required}
        mapped_rel_props = set(rm.properties.values())
        missing_required_props = sorted(required_rel_props - mapped_rel_props)
        for prop_name in missing_required_props:
            errors.append(
                ValidationIssue(
                    code="MISSING_REQUIRED_REL_PROPERTY",
                    severity="error",
                    message=(
                        f"관계 '{rm.rel_type}'의 필수 속성 '{prop_name}' 매핑이 누락되었습니다"
                    ),
                    path=f"relation_mappings[{idx}].properties",
                    dismissed_reason="missing_required_rel_property",
                )
            )

        for src_col, rel_prop in rm.properties.items():
            path = f"relation_mappings[{idx}].properties.{src_col}"
            if src_col not in header_set:
                if rel_prop in required_rel_props:
                    errors.append(
                        ValidationIssue(
                            code="MISSING_SOURCE_COLUMN",
                            severity="error",
                            message=f"컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다",
                            path=path,
                            dismissed_reason="missing_source_column",
                        )
                    )
                else:
                    warnings.append(
                        ValidationIssue(
                            code="OPTIONAL_REL_PROPERTY_SOURCE_MISSING",
                            severity="warning",
                            message=(
                                f"관계 속성 컬럼 '{src_col}'이 없어 해당 속성은 무시될 수 있습니다"
                            ),
                            path=path,
                        )
                    )
                continue

            data_type = rm.property_types.get(rel_prop, "string")
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
                        path=f"relation_mappings[{idx}].property_types.{rel_prop}",
                    )
                )

    used_columns = {cm.source_column for cm in normalized_mapping.column_mappings} | {
        ep.source_column for ep in normalized_mapping.extended_properties
    }
    for rm in normalized_mapping.relation_mappings:
        used_columns.update(rm.from_columns.values())
        used_columns.update(rm.to_columns.values())
        used_columns.update(rm.properties.keys())

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
            upload_id=req.upload_id,
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

    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(upload.file_key)

    sheet_names = get_sheet_names(content, upload.original_name)
    is_excel = len(sheet_names) > 0

    if req.sheet_name is not None:
        target_sheet = req.sheet_name
    elif is_excel:
        target_sheet = sheet_names[0]
    else:
        target_sheet = None

    headers, _ = extract_headers_and_rows(
        content,
        upload.original_name,
        sheet_name=target_sheet,
        max_rows=0,
    )

    record = MappingRecord(
        id=generate_uuid7(),
        upload_id=req.upload_id,
        name=req.name,
        sheet_name=req.sheet_name,
        original_headers=headers,
        mapping=validation.normalized_mapping.model_dump(),
        usage_count=0,
    )
    repo.create_mapping_record(db, record)
    db.flush()
    db.refresh(record)

    logger.info(
        "매핑 확정: mapping_id={mapping_id} name={name}",
        mapping_id=record.id,
        name=record.name,
    )
    return _to_mapping_response(record)


def list_mappings(db: Session) -> MappingListResponse:
    records = repo.list_mappings(db)
    return MappingListResponse(items=[_to_mapping_response(r) for r in records])


def get_mapping(db: Session, mapping_id: uuid.UUID) -> MappingResponse:
    record = repo.get_mapping_by_id(db, mapping_id)
    if record is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")
    return _to_mapping_response(record)


def _to_mapping_response(record: MappingRecord) -> MappingResponse:
    original_headers: list[str] = []
    if isinstance(record.original_headers, list):
        original_headers = [str(header) for header in record.original_headers]

    mapping_payload = MappingResult(
        column_mappings=[],
        relation_mappings=[],
        extended_properties=[],
    )
    if isinstance(record.mapping, dict):
        mapping_payload = MappingResult.model_validate(record.mapping)
    return MappingResponse(
        id=record.id,
        upload_id=record.upload_id,
        name=record.name,
        sheet_name=record.sheet_name,
        original_headers=original_headers,
        mapping=mapping_payload,
        usage_count=record.usage_count,
        created_at=record.created_at,
    )


def _build_editable_constraints() -> EditableConstraints:
    relation_catalog = []
    relation_property_catalog = []
    for rel in MANUFACTURING_ONTOLOGY.relationship_types:
        relation_catalog.append(
            {
                "rel_type": rel.rel_type,
                "from_label": rel.from_label,
                "to_label": rel.to_label,
                "description": rel.description,
            }
        )
        for prop in rel.properties:
            relation_property_catalog.append(
                {
                    "rel_type": rel.rel_type,
                    "property": prop.name,
                    "data_type": prop.data_type,
                    "required": prop.required,
                    "description": prop.description,
                }
            )

    return EditableConstraints(
        allowed_labels=[node.label for node in MANUFACTURING_ONTOLOGY.node_labels],
        allowed_properties_by_label={
            node.label: [prop.name for prop in node.properties]
            for node in MANUFACTURING_ONTOLOGY.node_labels
        },
        allowed_rel_types=[
            rel.rel_type for rel in MANUFACTURING_ONTOLOGY.relationship_types
        ],
        allowed_rel_properties_by_type={
            rel.rel_type: [prop.name for prop in rel.properties]
            for rel in MANUFACTURING_ONTOLOGY.relationship_types
        },
        merge_keys_by_label={
            node.label: list(node.merge_keys)
            for node in MANUFACTURING_ONTOLOGY.node_labels
        },
        relation_edit_mode="selectable",
        relation_catalog=relation_catalog,
        relation_property_catalog=relation_property_catalog,
    )


def _validate_relation_endpoint(
    errors: list[ValidationIssue],
    idx: int,
    direction: str,
    label: str,
    endpoint_columns: dict[str, str],
    headers: set[str],
    merge_keys_by_label: dict[str, set[str]],
) -> None:
    required_keys = merge_keys_by_label.get(label, set())
    for merge_key in required_keys:
        src_col = endpoint_columns.get(merge_key)
        if not src_col:
            errors.append(
                ValidationIssue(
                    code=(
                        "MISSING_FROM_ENDPOINT"
                        if direction == "from"
                        else "MISSING_TO_ENDPOINT"
                    ),
                    severity="error",
                    message=(
                        f"관계 {direction} 엔드포인트의 merge key '{merge_key}' 매핑이 누락되었습니다"
                    ),
                    path=(f"relation_mappings[{idx}].{direction}_columns.{merge_key}"),
                    dismissed_reason=(
                        "missing_from_endpoint"
                        if direction == "from"
                        else "missing_to_endpoint"
                    ),
                )
            )
            continue
        if src_col not in headers:
            errors.append(
                ValidationIssue(
                    code="MISSING_SOURCE_COLUMN",
                    severity="error",
                    message=f"컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다",
                    path=f"relation_mappings[{idx}].{direction}_columns.{merge_key}",
                    dismissed_reason="missing_source_column",
                )
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
