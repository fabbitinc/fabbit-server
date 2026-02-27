import time

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.ai_usage.service import check_bom_quota, log_usage
from app.modules.mapping import service as mapping_service
from app.modules.mapping.schemas import (
    MappingPreviewRequest,
    MappingPreviewResponse,
    SheetPreview,
    SkippedSheet,
)
from app.modules.ontology import service as ontology_service


@transactional(read_only=True)
def preview_mapping(
    db: Session,
    auth: AuthContext,
    req: MappingPreviewRequest,
) -> MappingPreviewResponse:
    check_bom_quota(auth.org_id)
    t_total = time.perf_counter()

    file = mapping_service.get_uploaded_file_or_raise(db, req.file_id)
    target_sheets = mapping_service.load_preview_targets(file, req.sheet_name)

    sheets: list[SheetPreview] = []
    skipped_sheets: list[SkippedSheet] = []
    first_headers: list[str] = []
    first_sample_rows: list[dict[str, object]] = []
    first_mapping = None

    for sheet in target_sheets:
        sheet_label = sheet or file.original_name
        try:
            headers, sample_rows = mapping_service.parse_sheet_preview(
                file,
                sheet_name=sheet,
                max_rows=5,
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

        log_usage(
            org_id=auth.org_id,
            user_id=auth.user_id,
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
