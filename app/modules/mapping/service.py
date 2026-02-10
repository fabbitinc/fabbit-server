"""매핑 도메인 서비스 레이어."""

import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.excel_parser import (
    extract_headers_and_rows,
    get_sheet_names,
)
from app.infrastructure.s3_client import S3Client
from app.modules.ai_usage.service import log_ai_usage
from app.modules.mapping import repository as repo
from app.modules.mapping.models import MappingRecord
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
    SheetPreview,
    SkippedSheet,
)
from app.modules.ontology import service as ontology_service

_s3 = S3Client()


def preview_mapping(
    db: Session,
    auth: AuthContext,
    req: MappingPreviewRequest,
) -> MappingPreviewResponse:
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
        try:
            headers, sample_rows = extract_headers_and_rows(
                content,
                upload.original_name,
                sheet_name=sheet,
                max_rows=5,
            )
        except Exception as e:
            if sheet is not None:
                skipped_sheets.append(SkippedSheet(
                    sheet_name=sheet,
                    reason=f"파싱 실패: {e}",
                ))
            continue

        if not headers:
            if sheet is not None:
                skipped_sheets.append(SkippedSheet(
                    sheet_name=sheet,
                    reason="헤더를 추출할 수 없습니다",
                ))
            continue

        mapping_result, llm_resp = ontology_service.generate_mapping(headers, sample_rows)

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
                skipped_sheets.append(SkippedSheet(
                    sheet_name=sheet,
                    reason="온톨로지에 매핑 가능한 컬럼이 없습니다",
                ))
            continue

        if sheet is not None:
            sheets.append(SheetPreview(
                sheet_name=sheet,
                headers=headers,
                sample_rows=sample_rows,
                mapping=mapping_result,
            ))

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

    logger.info(
        "매핑 미리보기 생성: upload_id={upload_id} sheets={sheet_count}개 skipped={skipped_count}개",
        upload_id=req.upload_id,
        sheet_count=len(sheets),
        skipped_count=len(skipped_sheets),
    )
    return MappingPreviewResponse(
        headers=first_headers,
        sample_rows=first_sample_rows,
        mapping=first_mapping,
        sheets=sheets,
        skipped_sheets=skipped_sheets,
    )


def confirm_mapping(
    db: Session,
    req: MappingConfirmRequest,
) -> MappingResponse:
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
        mapping=req.mapping.model_dump(),
        usage_count=0,
    )
    repo.create_mapping_record(db, record)
    db.commit()
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
    return MappingResponse(
        id=record.id,
        upload_id=record.upload_id,
        name=record.name,
        sheet_name=record.sheet_name,
        original_headers=record.original_headers,
        mapping=record.mapping,
        usage_count=record.usage_count,
        created_at=record.created_at,
    )
