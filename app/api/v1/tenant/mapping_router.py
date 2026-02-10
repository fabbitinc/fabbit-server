"""매핑 API 라우터.

업로드된 파일을 LLM이 분석하여 온톨로지 매핑을 제안하고,
사용자가 검토/확정하는 엔드포인트입니다.
"""

import uuid

from fastapi import APIRouter, Depends
from loguru import logger
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.infrastructure.excel_parser import extract_headers_and_rows
from app.infrastructure.s3_client import S3Client
from app.modules.mapping.models import MappingRecord
from app.modules.mapping.schemas import (
    MappingConfirmRequest,
    MappingListResponse,
    MappingPreviewRequest,
    MappingPreviewResponse,
    MappingResponse,
)
from app.modules.ontology import service as ontology_service
from app.modules.ai_usage.service import log_ai_usage
from app.modules.upload.models import Upload

router = APIRouter(prefix="/api/v1/mappings", tags=["mappings"])

_s3 = S3Client()


@router.post("/preview", response_model=MappingPreviewResponse)
def preview_mapping(
    req: MappingPreviewRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """업로드 파일 분석 → LLM 매핑 미리보기."""
    # 1. Upload 조회 + 상태 검증
    upload = db.query(Upload).filter(Upload.id == req.upload_id).first()
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if upload.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다. 먼저 업로드를 완료해주세요.",
            code="PRECONDITION_FAILED",
        )

    # 2. S3에서 파일 다운로드
    content = _s3.get_object(upload.file_key)

    # 3. 헤더 + 샘플 데이터 추출
    headers, sample_rows = extract_headers_and_rows(
        content, upload.original_name, header_row=req.header_row, max_rows=5
    )
    if not headers:
        raise AppError(
            message="파일에서 헤더를 추출할 수 없습니다",
            code="INVALID_INPUT",
        )

    # 4. LLM 매핑 생성
    mapping_result, llm_resp = ontology_service.generate_mapping(headers, sample_rows)

    # AI 사용량 로깅
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


@router.post("/confirm", response_model=MappingResponse)
def confirm_mapping(
    req: MappingConfirmRequest,
    db: Session = Depends(get_tenant_db),
):
    """매핑 확정 및 DB 저장."""
    # Upload 존재 검증
    upload = db.query(Upload).filter(Upload.id == req.upload_id).first()
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    # 헤더 추출 (original_headers 저장용)
    content = _s3.get_object(upload.file_key)
    headers, _ = extract_headers_and_rows(
        content, upload.original_name, header_row=req.header_row, max_rows=0
    )

    record = MappingRecord(
        id=generate_uuid7(),
        upload_id=req.upload_id,
        name=req.name,
        original_headers=headers,
        mapping=req.mapping.model_dump(),
        usage_count=0,
    )
    db.add(record)
    db.commit()
    db.refresh(record)

    logger.info(
        "매핑 확정: mapping_id={mapping_id} name={name}",
        mapping_id=record.id,
        name=record.name,
    )

    return MappingResponse(
        id=record.id,
        upload_id=record.upload_id,
        name=record.name,
        original_headers=record.original_headers,
        mapping=record.mapping,
        usage_count=record.usage_count,
        created_at=record.created_at,
    )


@router.get("", response_model=MappingListResponse)
def list_mappings(
    db: Session = Depends(get_tenant_db),
):
    """저장된 매핑 목록 조회 (최신순)."""
    records = db.query(MappingRecord).order_by(MappingRecord.created_at.desc()).all()
    return MappingListResponse(
        items=[
            MappingResponse(
                id=r.id,
                upload_id=r.upload_id,
                name=r.name,
                original_headers=r.original_headers,
                mapping=r.mapping,
                usage_count=r.usage_count,
                created_at=r.created_at,
            )
            for r in records
        ]
    )


@router.get("/{mapping_id}", response_model=MappingResponse)
def get_mapping(
    mapping_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    """매핑 상세 조회."""
    record = db.query(MappingRecord).filter(MappingRecord.id == mapping_id).first()
    if record is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")

    return MappingResponse(
        id=record.id,
        upload_id=record.upload_id,
        name=record.name,
        original_headers=record.original_headers,
        mapping=record.mapping,
        usage_count=record.usage_count,
        created_at=record.created_at,
    )
