"""매핑 도메인 서비스 레이어."""

import uuid

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.excel_parser import extract_headers_and_rows
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
    headers, sample_rows = extract_headers_and_rows(
        content,
        upload.original_name,
        header_row=req.header_row,
        max_rows=5,
    )
    if not headers:
        raise AppError(
            message="파일에서 헤더를 추출할 수 없습니다",
            code="INVALID_INPUT",
        )

    mapping_result, llm_resp = ontology_service.generate_mapping(headers, sample_rows)

    log_ai_usage(
        org_id=auth.org_id,
        user_id=auth.account_id,
        feature="mapping:preview",
        model=llm_resp.model,
        input_tokens=llm_resp.input_tokens,
        output_tokens=llm_resp.output_tokens,
    )

    logger.info(
        "매핑 미리보기 생성: upload_id={upload_id} headers={header_count}개 mappings={mapping_count}개",
        upload_id=req.upload_id,
        header_count=len(headers),
        mapping_count=len(mapping_result.column_mappings),
    )
    return MappingPreviewResponse(
        headers=headers,
        sample_rows=sample_rows,
        mapping=mapping_result,
    )


def confirm_mapping(
    db: Session,
    req: MappingConfirmRequest,
) -> MappingResponse:
    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    content = _s3.get_object(upload.file_key)
    headers, _ = extract_headers_and_rows(
        content,
        upload.original_name,
        header_row=req.header_row,
        max_rows=0,
    )

    record = MappingRecord(
        id=generate_uuid7(),
        upload_id=req.upload_id,
        name=req.name,
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
        original_headers=record.original_headers,
        mapping=record.mapping,
        usage_count=record.usage_count,
        created_at=record.created_at,
    )
