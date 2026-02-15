"""도면 분석 도메인 서비스 레이어."""

import json
import uuid
from datetime import datetime, timezone

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.database import create_tenant_session, generate_uuid7
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.image_converter import (
    detect_drawing_type,
    ensure_webp,
    pdf_extract_text,
    pdf_to_images,
)
from app.infrastructure.llm_client import vision_completion_with_usage
from app.infrastructure.s3_client import S3Client
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.drawing import repository as repo
from app.modules.drawing.models import DrawingAnalysisRecord
from app.modules.drawing.prompts import (
    DRAWING_ANALYSIS_SYSTEM_PROMPT,
    DRAWING_ANALYSIS_USER_MESSAGE,
    MULTI_PAGE_USER_MESSAGE,
    TEXT_ASSISTED_MULTI_PAGE_USER_MESSAGE,
    TEXT_ASSISTED_USER_MESSAGE,
)
from app.modules.drawing.schemas import (
    DrawingAnalysisResponse,
    DrawingAnalysisListResponse,
    DrawingAnalyzeRequest,
    DrawingAnalyzeResponse,
    DrawingAnalysisResult,
    DrawingConfirmRequest,
    DrawingSynthesisJobResponse,
    DrawingSynthesisStartRequest,
    ExtractedPart,
    MatchingReport,
    PartConflict,
    PartMatch,
)
from app.modules.ai_usage.service import log_ai_usage
from app.modules.ontology.cypher_utils import escape_cypher_value

_s3 = S3Client()


@transactional(read_only=True)
def analyze_drawing(
    db: Session,
    auth: AuthContext,
    req: DrawingAnalyzeRequest,
) -> DrawingAnalyzeResponse:
    """도면 분석 미리보기 — Vision LLM으로 표제란 + 부품 목록 추출."""
    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")
    if upload.status != "UPLOADED":
        raise AppError(message="업로드가 완료되지 않았습니다", code="PRECONDITION_FAILED")

    # S3에서 파일 다운로드
    content = _s3.get_object(upload.file_key)

    # 파일 타입 감지 + 이미지 변환
    file_type = detect_drawing_type(upload.original_name)
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
                    page_count=len(images), extracted_text=formatted_text,
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
    matching_report = _match_with_existing_graph(
        db, schema_name, analysis.parts
    )

    logger.info(
        "도면 분석 완료: upload_id={uid} pages={pages} parts={parts} "
        "matched={matched} new={new} conflicts={conflicts} "
        "method={method} tokens=in:{in_tok}/out:{out_tok}",
        uid=req.upload_id,
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
        upload_id=req.upload_id,
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
    upload = repo.get_upload_by_id(db, req.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    record_id = generate_uuid7()
    record = repo.create_analysis_record(
        db=db,
        record_id=record_id,
        upload_id=req.upload_id,
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

    upload = repo.get_upload_by_id(db, record.upload_id)
    if upload is None:
        raise AppError(message="업로드를 찾을 수 없습니다", code="NOT_FOUND")

    job = repo.create_synthesis_job(
        db=db,
        job_id=generate_uuid7(),
        analysis_id=record.id,
    )
    db.flush()
    db.refresh(job)

    schema_name = org_id_to_schema(auth.org_id)
    add_background_task(
        _run_drawing_synthesis,
        job_id=job.id,
        schema_name=schema_name,
        graph_name=schema_name,
        analysis_json=record.analysis,
        file_key=upload.file_key,
    )

    logger.info(
        "도면 합성 시작: job_id={jid} analysis_id={aid}",
        jid=job.id,
        aid=record.id,
    )
    return _to_job_response(job)


@transactional(read_only=True)
def get_synthesis_job(
    db: Session, job_id: uuid.UUID
) -> DrawingSynthesisJobResponse:
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

        # 1. Drawing 노드 MERGE
        tb = analysis.title_block
        drawing_number = tb.drawing_number
        if not drawing_number:
            drawing_number = f"DWG-{job_id.hex[:8]}"
            logger.warning("도면번호 없음, 자동 생성: {dn}", dn=drawing_number)

        drawing_props = _build_drawing_props(tb, file_key)

        try:
            repo.merge_drawing_node(db, graph_name, drawing_number, drawing_props)
            nodes_created += 1
        except Exception as e:
            errors.append(f"Drawing 노드 MERGE 실패: {e}")
            logger.error("Drawing MERGE 실패: {err}", err=e)

        # 2. Part 노드 MERGE + DEFINED_BY 관계
        for part in analysis.parts:
            if not part.part_number:
                continue

            try:
                part_props = _build_part_props(part)
                repo.merge_part_node(db, graph_name, part.part_number, part_props)
                nodes_created += 1
            except Exception as e:
                errors.append(f"Part MERGE 실패 ({part.part_number}): {e}")
                logger.warning("Part MERGE 실패: {pn} - {err}", pn=part.part_number, err=e)
                continue

            try:
                repo.merge_defined_by(db, graph_name, part.part_number, drawing_number)
                rels_created += 1
            except Exception as e:
                errors.append(f"DEFINED_BY 관계 실패 ({part.part_number}): {e}")
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


def _build_drawing_props(tb, file_key: str) -> dict[str, str]:
    """표제란 데이터 → Cypher SET 절용 속성 딕셔너리."""
    props: dict[str, str] = {}
    if tb.name:
        props["name"] = f"'{escape_cypher_value(tb.name)}'"
    if tb.version:
        props["version"] = f"'{escape_cypher_value(tb.version)}'"
    if file_key:
        props["file_path"] = f"'{escape_cypher_value(file_key)}'"
    if tb.author:
        props["_ext_author"] = f"'{escape_cypher_value(tb.author)}'"
    if tb.date:
        props["_ext_date"] = f"'{escape_cypher_value(tb.date)}'"
    if tb.sheet_info:
        props["_ext_sheet_info"] = f"'{escape_cypher_value(tb.sheet_info)}'"
    for key, val in tb.additional.items():
        safe_key = key.lower().replace(" ", "_")
        props[f"_ext_{safe_key}"] = f"'{escape_cypher_value(val)}'"
    return props


def _build_part_props(part: ExtractedPart) -> dict[str, str]:
    """추출된 부품 데이터 → Cypher SET 절용 속성 딕셔너리."""
    props: dict[str, str] = {}
    if part.name:
        props["name"] = f"'{escape_cypher_value(part.name)}'"
    if part.value:
        props["_ext_value"] = f"'{escape_cypher_value(part.value)}'"
    if part.package:
        props["_ext_package"] = f"'{escape_cypher_value(part.package)}'"
    if part.reference_designator:
        props["_ext_reference_designator"] = f"'{escape_cypher_value(part.reference_designator)}'"
    return props


def _to_analysis_response(record: DrawingAnalysisRecord) -> DrawingAnalysisResponse:
    return DrawingAnalysisResponse(
        id=record.id,
        upload_id=record.upload_id,
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
