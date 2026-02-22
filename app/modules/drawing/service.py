"""도면 분석 도메인 서비스 레이어."""

import json
import os
import uuid
from datetime import datetime, timezone

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.config import settings
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.drawing_converter_client import drawing_converter_client
from app.infrastructure.image_converter import (
    detect_drawing_type,
    ensure_webp,
    pdf_extract_text,
    pdf_to_images,
)
from app.infrastructure.llm_client import vision_completion_with_usage
from app.infrastructure.s3_client import s3_client
from app.modules.ai_usage.service import log_ai_usage
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.drawing import repository as repo
from app.modules.drawing.constants import ALLOWED_DRAWING_EXTENSIONS, ConversionStatus
from app.modules.drawing.models import Drawing, DrawingAnalysisRecord
from app.modules.drawing.prompts import (
    DRAWING_ANALYSIS_SYSTEM_PROMPT,
    DRAWING_ANALYSIS_USER_MESSAGE,
    MULTI_PAGE_USER_MESSAGE,
    TEXT_ASSISTED_MULTI_PAGE_USER_MESSAGE,
    TEXT_ASSISTED_USER_MESSAGE,
)
from app.modules.drawing.schemas import (
    BatchConversionResultRequest,
    BulkRegisterDrawingFailure,
    BulkRegisterDrawingRequest,
    BulkRegisterDrawingResponse,
    ConversionResultRequest,
    DrawingAnalysisListResponse,
    DrawingAnalysisResponse,
    DrawingAnalysisResult,
    DrawingAnalyzeRequest,
    DrawingAnalyzeResponse,
    DrawingConfirmRequest,
    DrawingListResponse,
    DrawingSummary,
    DrawingSynthesisJobResponse,
    DrawingSynthesisStartRequest,
    ExtractedPart,
    MatchingReport,
    PartConflict,
    PartMatch,
    RegisterDrawingResponse,
)
from app.modules.file import repository as file_repo
from app.modules.file import service as file_service
from app.modules.file.constants import FileStatus
from app.modules.file.models import File
from app.modules.part import repository as part_repo

_s3 = s3_client
_converter = drawing_converter_client

# ── Drawing 등록 ──


@transactional
def register_drawing(
    db: Session,
    auth: AuthContext,
    file_id: uuid.UUID,
    part_id: uuid.UUID,
    add_background_task,
) -> RegisterDrawingResponse:
    """파일을 Drawing으로 등록하고 Part에 연결.

    모든 파일을 Converter에 전송합니다 (DWG→PDF+썸네일, PDF→썸네일 등).
    변환 판단은 Converter가 수행합니다.
    """
    file = file_repo.get_file_by_id(db, file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")
    if file.status != FileStatus.UPLOADED:
        raise AppError(
            message="업로드가 완료되지 않은 파일입니다", code="PRECONDITION_FAILED"
        )

    part = part_repo.get_by_id(db, part_id)
    if part is None:
        raise AppError(message="Part를 찾을 수 없습니다", code="NOT_FOUND")

    drawing = _create_drawing(db, file)

    part.assign_drawing(drawing.id)
    db.flush()

    if _converter.enabled:
        add_background_task(
            _request_conversion,
            file_id=file.id,
            file_key=file.file_key,
            drawing_id=drawing.id,
            tenant_schema=org_id_to_schema(auth.org_id),
        )

    return RegisterDrawingResponse(
        drawing_id=drawing.id,
        drawing_number=drawing.drawing_number,
        name=drawing.name,
        conversion_status=drawing.conversion_status,
    )


@transactional
def bulk_register_drawings(
    db: Session,
    auth: AuthContext,
    req: BulkRegisterDrawingRequest,
    add_background_task,
) -> BulkRegisterDrawingResponse:
    """도면 대량 등록.

    각 항목을 개별 처리하며, 실패한 항목은 failed 목록에 포함합니다.
    변환 요청은 BackgroundTask로 트랜잭션 커밋 후 배치 실행됩니다.
    """
    items: list[RegisterDrawingResponse] = []
    failed: list[BulkRegisterDrawingFailure] = []
    pending_conversions: list[dict] = []

    for item in req.items:
        try:
            file = file_repo.get_file_by_id(db, item.file_id)
            if file is None:
                failed.append(
                    BulkRegisterDrawingFailure(
                        file_id=item.file_id,
                        reason="파일을 찾을 수 없습니다",
                    )
                )
                continue
            if file.status != FileStatus.UPLOADED:
                failed.append(
                    BulkRegisterDrawingFailure(
                        file_id=item.file_id,
                        reason="업로드가 완료되지 않은 파일입니다",
                    )
                )
                continue

            drawing = _create_drawing(db, file)

            # Part 연결 (part_id 제공 시)
            if item.part_id is not None:
                part = part_repo.get_by_id(db, item.part_id)
                if part is None:
                    failed.append(
                        BulkRegisterDrawingFailure(
                            file_id=item.file_id,
                            reason="Part를 찾을 수 없습니다",
                        )
                    )
                    continue
                part.assign_drawing(drawing.id)
                db.flush()

            pending_conversions.append(
                {
                    "file_id": file.id,
                    "file_key": file.file_key,
                    "drawing_id": drawing.id,
                }
            )

            items.append(
                RegisterDrawingResponse(
                    drawing_id=drawing.id,
                    drawing_number=drawing.drawing_number,
                    name=drawing.name,
                    conversion_status=drawing.conversion_status,
                )
            )
        except Exception as e:
            logger.warning(
                "대량 도면 등록 개별 실패: file_id={file_id} error={error}",
                file_id=item.file_id,
                error=str(e),
            )
            failed.append(
                BulkRegisterDrawingFailure(
                    file_id=item.file_id,
                    reason=str(e),
                )
            )

    # 변환 요청은 커밋 후 background task로 배치 실행
    if _converter.enabled and pending_conversions:
        add_background_task(
            _request_batch_conversion,
            items=pending_conversions,
            tenant_schema=org_id_to_schema(auth.org_id),
        )

    logger.info(
        "대량 도면 등록: 성공={ok}건 실패={fail}건",
        ok=len(items),
        fail=len(failed),
    )
    return BulkRegisterDrawingResponse(items=items, failed=failed)


@transactional
def delete_drawing(db: Session, auth: AuthContext, part_id: uuid.UUID) -> None:
    """Part에 연결된 도면 삭제.

    Drawing 레코드 삭제 + 연결 파일 소프트 삭제.
    """
    part = part_repo.get_by_id(db, part_id)
    if part is None:
        raise AppError(message="Part를 찾을 수 없습니다", code="NOT_FOUND")
    if part.drawing_id is None:
        raise AppError(message="연결된 도면이 없습니다", code="NOT_FOUND")

    drawing = repo.get_drawing_by_id(db, part.drawing_id)
    if drawing is None:
        raise AppError(message="도면을 찾을 수 없습니다", code="NOT_FOUND")

    # 1. Part 연결 해제
    part.unassign_drawing()

    # 2. 연결 파일 소프트 삭제 (original, pdf, thumbnail)
    file_ids = [
        fid
        for fid in [
            drawing.original_file_id,
            drawing.pdf_file_id,
            drawing.thumbnail_file_id,
        ]
        if fid is not None
    ]
    # 중복 제거 (pdf_file_id == original_file_id인 경우)
    unique_file_ids = list(set(file_ids))
    if unique_file_ids:
        file_service.soft_delete_files(db, unique_file_ids)

    # 3. Drawing 레코드 삭제 (hard delete)
    db.delete(drawing)


def _validate_drawing_file(file: File) -> None:
    """도면 등록 가능한 파일 형식인지 검증."""
    _, ext = os.path.splitext(file.original_name)
    if ext.lower() not in ALLOWED_DRAWING_EXTENSIONS:
        raise AppError(
            message=f"도면으로 등록할 수 없는 파일 형식입니다: {ext}",
            code="UNSUPPORTED_FORMAT",
        )


def _create_drawing(db: Session, file: File) -> Drawing:
    """Drawing 레코드 생성 (PENDING). 변환 판단은 Converter가 수행."""
    _validate_drawing_file(file)
    drawing = Drawing.create_pending(
        original_file_id=file.id,
        original_file_key=file.file_key,
        original_name=file.original_name,
    )
    db.add(drawing)
    db.flush()
    return drawing


def _request_conversion(
    file_id: uuid.UUID,
    file_key: str,
    drawing_id: uuid.UUID,
    tenant_schema: str,
) -> None:
    """Converter에 단건 변환 요청 (BackgroundTask에서 실행)."""
    callback_url = f"{settings.base_api_url}/api/v1/internal/webhooks/drawing-converter"
    try:
        _converter.request_conversion(
            tenant_schema=tenant_schema,
            callback_url=callback_url,
            file_id=file_id,
            file_key=file_key,
            drawing_id=drawing_id,
        )
    except Exception:
        logger.warning(
            "변환 요청 실패: file_id={file_id} drawing_id={drawing_id}",
            file_id=file_id,
            drawing_id=drawing_id,
        )


def _request_batch_conversion(
    items: list[dict],
    tenant_schema: str,
) -> None:
    """Converter에 배치 변환 요청 (BackgroundTask에서 실행)."""
    callback_url = (
        f"{settings.base_api_url}/api/v1/internal/webhooks/drawing-converter/batch"
    )
    try:
        _converter.request_batch_conversion(
            items=items,
            tenant_schema=tenant_schema,
            callback_url=callback_url,
        )
    except Exception:
        logger.warning(
            "배치 변환 요청 실패: count={count}",
            count=len(items),
        )


# ── 변환 결과 처리 ──


def handle_conversion_result(req: ConversionResultRequest) -> None:
    """Webhook 단건 변환 결과를 Drawing에 반영.

    PDF/thumbnail은 각각 독립 File 레코드로 생성.
    webhook은 테넌트 인증 없이 호출되므로 create_tenant_session을 사용합니다.
    """
    db = create_tenant_session(req.tenant_schema)
    try:
        _apply_conversion_result(
            db,
            drawing_id=req.drawing_id,
            file_id=req.file_id,
            status=req.status,
            pdf_key=req.pdf_key,
            pdf_content_type=req.pdf_content_type,
            pdf_size=req.pdf_size,
            thumbnail_key=req.thumbnail_key,
            thumbnail_content_type=req.thumbnail_content_type,
            thumbnail_size=req.thumbnail_size,
            error=req.error,
        )
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def handle_batch_conversion_result(req: BatchConversionResultRequest) -> None:
    """Webhook 배치 변환 결과를 Drawing에 반영.

    각 항목을 개별 처리하며, 하나가 실패해도 나머지는 반영합니다.
    """
    db = create_tenant_session(req.tenant_schema)
    try:
        for item in req.items:
            try:
                _apply_conversion_result(
                    db,
                    drawing_id=item.drawing_id,
                    file_id=item.file_id,
                    status=item.status,
                    pdf_key=item.pdf_key,
                    pdf_content_type=item.pdf_content_type,
                    pdf_size=item.pdf_size,
                    thumbnail_key=item.thumbnail_key,
                    thumbnail_content_type=item.thumbnail_content_type,
                    thumbnail_size=item.thumbnail_size,
                    error=item.error,
                )
            except Exception:
                logger.error(
                    "배치 변환 결과 개별 반영 실패: drawing_id={drawing_id}",
                    drawing_id=item.drawing_id,
                )
        db.commit()
    except Exception:
        db.rollback()
        raise
    finally:
        db.close()


def _apply_conversion_result(
    db: Session,
    drawing_id: uuid.UUID,
    file_id: uuid.UUID,
    status: str,
    pdf_key: str | None,
    pdf_content_type: str | None,
    pdf_size: int | None,
    thumbnail_key: str | None,
    thumbnail_content_type: str | None,
    thumbnail_size: int | None,
    error: str | None,
) -> None:
    """단일 변환 결과를 Drawing에 반영 (공통 로직)."""
    drawing = repo.get_drawing_by_id(db, drawing_id)
    if drawing is None:
        logger.warning(
            "변환 결과 수신 — Drawing 없음: drawing_id={drawing_id} file_id={file_id}",
            drawing_id=drawing_id,
            file_id=file_id,
        )
        return

    if status == ConversionStatus.COMPLETED:
        # PDF File 레코드 — 원본과 동일하면 재사용, 다르면 새로 생성
        pdf_file_id = None
        if pdf_key:
            if pdf_key == drawing.original_file_key:
                pdf_file_id = drawing.original_file_id
            else:
                pdf_file = file_repo.create_file_record(
                    db,
                    file_id=generate_uuid7(),
                    original_name=f"{drawing.name}.pdf",
                    file_key=pdf_key,
                    content_type=pdf_content_type or "application/pdf",
                    file_size=pdf_size or 0,
                    owner_type="drawing",
                    owner_id=drawing.id,
                )
                pdf_file.mark_uploaded()
                pdf_file_id = pdf_file.id

        # Thumbnail File 레코드 — 원본과 동일하면 재사용, 다르면 새로 생성
        thumbnail_file_id = None
        if thumbnail_key:
            if thumbnail_key == drawing.original_file_key:
                thumbnail_file_id = drawing.original_file_id
            else:
                thumb_file = file_repo.create_file_record(
                    db,
                    file_id=generate_uuid7(),
                    original_name=f"{drawing.name}_thumb.webp",
                    file_key=thumbnail_key,
                    content_type=thumbnail_content_type or "image/webp",
                    file_size=thumbnail_size or 0,
                    owner_type="drawing",
                    owner_id=drawing.id,
                )
                thumb_file.mark_uploaded()
                thumbnail_file_id = thumb_file.id

        drawing.complete_conversion(
            pdf_file_id=pdf_file_id,
            pdf_key=pdf_key,
            thumbnail_file_id=thumbnail_file_id,
            thumbnail_key=thumbnail_key,
        )
    else:
        drawing.fail_conversion()
        logger.warning(
            "변환 실패: drawing_id={drawing_id} error={error}",
            drawing_id=drawing_id,
            error=error,
        )

    logger.info(
        "변환 결과 반영: drawing_id={drawing_id} status={status}",
        drawing_id=drawing_id,
        status=status,
    )


# ── Drawing 목록 검색 ──


@transactional(read_only=True)
def list_drawings(
    db: Session,
    auth: AuthContext,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> DrawingListResponse:
    """Drawing 목록 페이징 조회."""
    drawings, total = repo.list_drawings_paginated(
        db, search=search, offset=offset, limit=limit
    )

    items = [
        DrawingSummary(
            id=d.id,
            drawing_number=d.drawing_number,
            name=d.name,
            version=d.version,
            status=d.status,
            original_file_key=d.original_file_key,
            pdf_key=d.pdf_key,
            thumbnail_key=d.thumbnail_key,
            conversion_status=d.conversion_status,
        )
        for d in drawings
    ]

    return DrawingListResponse(total=total, offset=offset, limit=limit, items=items)


@transactional(read_only=True)
def analyze_drawing(
    db: Session,
    auth: AuthContext,
    req: DrawingAnalyzeRequest,
) -> DrawingAnalyzeResponse:
    """도면 분석 미리보기 — Vision LLM으로 표제란 + 부품 목록 추출."""
    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")
    if file.status != "UPLOADED":
        raise AppError(
            message="업로드가 완료되지 않았습니다", code="PRECONDITION_FAILED"
        )

    # S3에서 파일 다운로드
    content = _s3.get_object(file.file_key)

    # 파일 타입 감지 + 이미지 변환
    file_type = detect_drawing_type(file.original_name)
    if file_type == "unsupported":
        raise AppError(
            message="지원하지 않는 파일 형식입니다. PDF, PNG, JPG만 지원합니다.",
            code="UNSUPPORTED_FORMAT",
        )

    if file_type == "pdf":
        images = pdf_to_images(content)  # WebP
        if req.page_range:
            images = _filter_pages(images, req.page_range)

        # 텍스트 추출 시도 (보조 데이터)
        extracted = pdf_extract_text(content)

        if extracted["has_meaningful_text"]:
            # 벡터 PDF → 이미지 + 추출 텍스트 → Vision LLM
            formatted_text = _format_extracted_text(extracted)
            if len(images) > 1:
                user_msg = TEXT_ASSISTED_MULTI_PAGE_USER_MESSAGE.format(
                    page_count=len(images),
                    extracted_text=formatted_text,
                )
            else:
                user_msg = TEXT_ASSISTED_USER_MESSAGE.format(
                    extracted_text=formatted_text,
                )
            method = "text_assisted_vision"
        else:
            # 스캔 PDF → 이미지만 → Vision LLM
            user_msg = (
                MULTI_PAGE_USER_MESSAGE.format(page_count=len(images))
                if len(images) > 1
                else DRAWING_ANALYSIS_USER_MESSAGE
            )
            method = "vision_llm"
    else:
        images = [ensure_webp(content)]
        user_msg = DRAWING_ANALYSIS_USER_MESSAGE
        method = "vision_llm"

    page_count = len(images)
    if page_count == 0:
        raise AppError(message="변환 가능한 페이지가 없습니다", code="EMPTY_CONTENT")

    # Vision LLM 호출
    llm_resp = vision_completion_with_usage(
        system_prompt=DRAWING_ANALYSIS_SYSTEM_PROMPT,
        user_message=user_msg,
        images=images,
        reasoning_effort="low",
        response_format={"type": "json_object"},
    )

    # JSON 파싱
    try:
        raw = json.loads(llm_resp.content)
        analysis = DrawingAnalysisResult(**raw)
    except (json.JSONDecodeError, Exception) as e:
        logger.error("도면 분석 JSON 파싱 실패: {err}", err=e)
        raise AppError(
            message="도면 분석 결과를 파싱할 수 없습니다",
            code="LLM_PARSE_ERROR",
        )

    log_ai_usage(
        org_id=auth.org_id,
        user_id=auth.account_id,
        feature="drawing:analyze",
        model=llm_resp.model,
        input_tokens=llm_resp.input_tokens,
        output_tokens=llm_resp.output_tokens,
    )

    # 기존 BOM 데이터와 매칭
    schema_name = org_id_to_schema(auth.org_id)
    matching_report = _match_with_existing_graph(db, schema_name, analysis.parts)

    logger.info(
        "도면 분석 완료: file_id={fid} pages={pages} parts={parts} "
        "matched={matched} new={new} conflicts={conflicts} "
        "method={method} tokens=in:{in_tok}/out:{out_tok}",
        fid=req.file_id,
        pages=page_count,
        parts=len(analysis.parts),
        matched=len(matching_report.matched_parts),
        new=len(matching_report.new_parts),
        conflicts=len(matching_report.conflicting_parts),
        method=method,
        in_tok=llm_resp.input_tokens,
        out_tok=llm_resp.output_tokens,
    )

    return DrawingAnalyzeResponse(
        file_id=req.file_id,
        page_count=page_count,
        analysis=analysis,
        matching_report=matching_report,
        extraction_method=method,
    )


@transactional
def confirm_analysis(
    db: Session,
    req: DrawingConfirmRequest,
) -> DrawingAnalysisResponse:
    """분석 결과를 확정하고 DB에 저장."""
    file = repo.get_file_by_id(db, req.file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")

    record_id = generate_uuid7()
    record = repo.create_analysis_record(
        db=db,
        record_id=record_id,
        file_id=req.file_id,
        name=req.name,
        analysis=req.analysis.model_dump(),
        page_count=1,  # 확정 시점에는 분석 결과만 저장
    )

    # server_default 값(created_at)을 DB에서 받아오기 위해 flush + refresh
    db.flush()
    db.refresh(record)

    return _to_analysis_response(record)


@transactional(read_only=True)
def list_analyses(db: Session) -> DrawingAnalysisListResponse:
    """분석 레코드 목록 조회."""
    records = repo.list_analysis_records(db)
    return DrawingAnalysisListResponse(
        items=[_to_analysis_response(r) for r in records]
    )


@transactional(read_only=True)
def get_analysis(db: Session, analysis_id: uuid.UUID) -> DrawingAnalysisResponse:
    """분석 레코드 상세 조회."""
    record = repo.get_analysis_by_id(db, analysis_id)
    if record is None:
        raise AppError(message="분석 레코드를 찾을 수 없습니다", code="NOT_FOUND")
    return _to_analysis_response(record)


@transactional
def start_drawing_synthesis(
    db: Session,
    auth: AuthContext,
    req: DrawingSynthesisStartRequest,
    add_background_task,
) -> DrawingSynthesisJobResponse:
    """도면 합성 시작 — Background task로 그래프 적재."""
    record = repo.get_analysis_by_id(db, req.analysis_id)
    if record is None:
        raise AppError(message="분석 레코드를 찾을 수 없습니다", code="NOT_FOUND")

    file = repo.get_file_by_id(db, record.file_id)
    if file is None:
        raise AppError(message="파일을 찾을 수 없습니다", code="NOT_FOUND")

    job = repo.create_synthesis_job(
        db=db,
        job_id=generate_uuid7(),
        analysis_id=record.id,
    )
    db.flush()
    db.refresh(job)

    schema_name = org_id_to_schema(auth.org_id)
    add_background_task(
        guarded(_run_drawing_synthesis),
        job_id=job.id,
        schema_name=schema_name,
        graph_name=schema_name,
        analysis_json=record.analysis,
        file_key=file.file_key,
        file_id=file.id,
    )

    logger.info(
        "도면 합성 시작: job_id={jid} analysis_id={aid}",
        jid=job.id,
        aid=record.id,
    )
    return _to_job_response(job)


@transactional(read_only=True)
def get_synthesis_job(db: Session, job_id: uuid.UUID) -> DrawingSynthesisJobResponse:
    """합성 작업 상태 조회."""
    job = repo.get_synthesis_job_by_id(db, job_id)
    if job is None:
        raise AppError(message="합성 작업을 찾을 수 없습니다", code="NOT_FOUND")
    return _to_job_response(job)


# ── 내부 함수 ──


def _format_extracted_text(extracted: dict) -> str:
    """추출된 PDF 텍스트 데이터를 LLM이 이해하기 쉬운 형태로 포맷."""
    parts: list[str] = []

    for page_data in extracted["pages"]:
        page_num = page_data["page_num"]
        parts.append(f"### 페이지 {page_num}")

        # 텍스트 블록
        if page_data["text_blocks"]:
            texts = [b["text"] for b in page_data["text_blocks"]]
            parts.append(" | ".join(texts))

        # 테이블 → 마크다운 테이블
        for table_idx, table in enumerate(page_data["tables"]):
            if not table:
                continue
            parts.append(f"\n**테이블 {table_idx + 1}**")
            header = table[0]
            parts.append("| " + " | ".join(str(c) for c in header) + " |")
            parts.append("| " + " | ".join("---" for _ in header) + " |")
            for row in table[1:]:
                parts.append("| " + " | ".join(str(c) for c in row) + " |")

        parts.append("")  # 빈 줄

    return "\n".join(parts)


def _filter_pages(images: list[bytes], page_range: str) -> list[bytes]:
    """page_range 문자열("1-3", "2,4,5")로 페이지 필터링."""
    indices: set[int] = set()
    for part in page_range.split(","):
        part = part.strip()
        if "-" in part:
            start, end = part.split("-", 1)
            for i in range(int(start) - 1, int(end)):
                indices.add(i)
        else:
            indices.add(int(part) - 1)

    return [img for i, img in enumerate(images) if i in indices]


def _match_with_existing_graph(
    db: Session,
    graph_name: str,
    parts: list[ExtractedPart],
) -> MatchingReport:
    """추출된 부품 목록과 기존 그래프의 Part 노드를 비교."""
    part_numbers = [p.part_number for p in parts if p.part_number]
    if not part_numbers:
        return MatchingReport(new_parts=parts)

    try:
        existing = repo.find_existing_parts_by_numbers(db, graph_name, part_numbers)
    except Exception as e:
        logger.warning("기존 Part 조회 실패 (그래프 미초기화 가능): {err}", err=e)
        return MatchingReport(new_parts=parts)

    matched: list[PartMatch] = []
    new_parts: list[ExtractedPart] = []
    conflicts: list[PartConflict] = []

    for part in parts:
        if not part.part_number or part.part_number not in existing:
            new_parts.append(part)
            continue

        ex = existing[part.part_number]
        matched.append(
            PartMatch(
                extracted=part,
                existing_part_number=part.part_number,
                existing_name=ex.get("name"),
            )
        )

        # 속성 불일치 검사 (name)
        if part.name and ex.get("name") and part.name != ex["name"]:
            conflicts.append(
                PartConflict(
                    part_number=part.part_number,
                    field="name",
                    extracted_value=part.name,
                    existing_value=ex["name"],
                )
            )

    return MatchingReport(
        matched_parts=matched,
        new_parts=new_parts,
        conflicting_parts=conflicts,
    )


def _run_drawing_synthesis(
    job_id: uuid.UUID,
    schema_name: str,
    graph_name: str,
    analysis_json: dict,
    file_key: str,
    file_id: uuid.UUID,
) -> None:
    """Background task — 도면 분석 결과를 AGE 그래프에 적재."""
    db = create_tenant_session(schema_name)
    try:
        job = repo.get_synthesis_job_required(db, job_id)
        job.status = "PROCESSING"
        job.started_at = datetime.now(timezone.utc)
        db.commit()

        analysis = DrawingAnalysisResult(**analysis_json)
        nodes_created = 0
        rels_created = 0
        errors: list[str] = []

        # 1. Drawing 노드 upsert (RDS + Graph dual-write)
        tb = analysis.title_block
        drawing_number = tb.drawing_number
        if not drawing_number:
            drawing_number = f"DWG-{job_id.hex[:8]}"
            logger.warning("도면번호 없음, 자동 생성: {dn}", dn=drawing_number)

        drawing_props = _build_drawing_props(tb, file_key)

        try:
            repo.upsert_drawing(
                db,
                drawing_number,
                drawing_props,
                graph_name,
                original_file_id=file_id,
            )
            nodes_created += 1
        except Exception as e:
            errors.append(f"Drawing upsert 실패: {e}")
            logger.error("Drawing upsert 실패: {err}", err=e)

        # 2. Part 노드 upsert + DEFINED_BY 관계 (RDS + Graph dual-write)
        for part in analysis.parts:
            if not part.part_number:
                continue

            try:
                p_props = _build_part_props(part)
                part_repo.upsert_part(db, part.part_number, p_props, None, graph_name)
                nodes_created += 1
            except Exception as e:
                errors.append(f"Part upsert 실패 ({part.part_number}): {e}")
                logger.warning(
                    "Part upsert 실패: {pn} - {err}",
                    pn=part.part_number,
                    err=e,
                )
                continue

            try:
                part_repo.link_part_to_drawing(
                    db, graph_name, part.part_number, drawing_number
                )
                rels_created += 1
            except Exception as e:
                errors.append(f"DEFINED_BY 실패 ({part.part_number}): {e}")
                logger.warning(
                    "DEFINED_BY 실패: {pn} → {dn} - {err}",
                    pn=part.part_number,
                    dn=drawing_number,
                    err=e,
                )

        db.commit()

        # 작업 완료 상태 업데이트
        job.status = "COMPLETED"
        job.nodes_created = nodes_created
        job.relationships_created = rels_created
        job.errors = errors[:100]
        job.completed_at = datetime.now(timezone.utc)
        db.commit()

        logger.info(
            "도면 합성 완료: job_id={jid} 노드={nodes} 관계={rels} 에러={errs}",
            jid=job_id,
            nodes=nodes_created,
            rels=rels_created,
            errs=len(errors),
        )

    except Exception as error:
        logger.error("도면 합성 실패: job_id={jid} error={err}", jid=job_id, err=error)
        try:
            db.rollback()
            job = repo.get_synthesis_job_required(db, job_id)
            job.status = "FAILED"
            job.errors = [str(error)]
            job.completed_at = datetime.now(timezone.utc)
            db.commit()
        except Exception:
            logger.error("도면 합성 실패 상태 저장 오류: job_id={jid}", jid=job_id)
    finally:
        db.close()


def _build_drawing_props(tb, file_key: str) -> dict:
    """표제란 데이터 → Python dict (RDS upsert용)."""
    props: dict = {}
    if tb.name:
        props["name"] = tb.name
    if tb.version:
        props["version"] = tb.version
    if file_key:
        props["file_path"] = file_key
    if tb.author:
        props["_ext_author"] = tb.author
    if tb.date:
        props["_ext_date"] = tb.date
    if tb.sheet_info:
        props["_ext_sheet_info"] = tb.sheet_info
    for key, val in tb.additional.items():
        safe_key = key.lower().replace(" ", "_")
        props[f"_ext_{safe_key}"] = val
    return props


def _build_part_props(part: ExtractedPart) -> dict:
    """추출된 부품 데이터 → Python dict (RDS upsert용)."""
    props: dict = {}
    if part.name:
        props["name"] = part.name
    if part.value:
        props["_ext_value"] = part.value
    if part.package:
        props["_ext_package"] = part.package
    if part.reference_designator:
        props["_ext_reference_designator"] = part.reference_designator
    return props


def _to_analysis_response(record: DrawingAnalysisRecord) -> DrawingAnalysisResponse:
    return DrawingAnalysisResponse(
        id=record.id,
        file_id=record.file_id,
        name=record.name,
        analysis=record.analysis,
        page_count=record.page_count,
        created_at=record.created_at,
    )


def _to_job_response(job) -> DrawingSynthesisJobResponse:
    return DrawingSynthesisJobResponse(
        id=job.id,
        analysis_id=job.analysis_id,
        status=job.status,
        nodes_created=job.nodes_created,
        relationships_created=job.relationships_created,
        errors=job.errors,
        started_at=job.started_at,
        completed_at=job.completed_at,
        created_at=job.created_at,
    )
