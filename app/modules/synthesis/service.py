"""합성 도메인 서비스 레이어."""

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.background_worker import guarded
from app.core.database import generate_uuid7
from app.core.exceptions import AppError
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.schemas import MappingResult
from app.modules.synthesis import repository as repo
from app.modules.synthesis.mapper import to_job_response
from app.modules.synthesis.models import SynthesisJob
from app.modules.synthesis.pipeline import run_synthesis
from app.modules.mapping.constants import MappingScope
from app.modules.synthesis.schemas import (
    SynthesisBatchFailure,
    SynthesisBatchStartResponse,
    SynthesisStartRequest,
    SynthesisUploadItem,
)


def _get_rootless_labels(mapping: MappingResult) -> set[str]:
    """매핑에서 rootless relation의 target_label 집합 추출."""
    return {
        rm.target_label
        for rm in mapping.relation_mappings
        if not rm.node_columns and rm.rel_columns
    }


def _validate_root_context(
    scope: str,
    root_context: dict[str, str] | None,
    required_labels: set[str],
) -> None:
    """scope 기반 root_context 검증."""
    if scope in (MappingScope.PART_LIST, MappingScope.FULL_BOM):
        if root_context:
            raise AppError(
                message="이 매핑은 root_context가 필요하지 않습니다.",
                code="UNEXPECTED_ROOT_CONTEXT",
            )
    elif scope == MappingScope.ROOT_BOM:
        if not root_context:
            raise AppError(
                message="이 매핑은 ROOT_BOM입니다. root_context를 지정해주세요.",
                code="MISSING_ROOT_CONTEXT",
            )
        missing = required_labels - root_context.keys()
        if missing:
            raise AppError(
                message=f"root_context에 필요한 키가 누락되었습니다: {', '.join(sorted(missing))}",
                code="MISSING_ROOT_CONTEXT",
            )


def start_synthesis(
    db: Session,
    auth: AuthContext,
    req: SynthesisStartRequest,
    add_background_task,
) -> SynthesisBatchStartResponse:
    # 1. 매핑 조회
    record = repo.get_mapping_by_id(db, req.mapping_id)
    if record is None:
        raise AppError(message="매핑을 찾을 수 없습니다", code="NOT_FOUND")

    # 2. 최신 리비전 조회
    revision = repo.get_latest_revision(db, record.id)
    if revision is None:
        raise AppError(message="매핑 리비전을 찾을 수 없습니다", code="NOT_FOUND")

    # 3. 모든 upload에 대해 root_context 검증 (scope 기반)
    mapping_for_scope = MappingResult(**revision.mapping)
    required_labels = _get_rootless_labels(mapping_for_scope)
    for item in req.uploads:
        _validate_root_context(record.scope, item.root_context, required_labels)

    # 4. upload 루프
    schema_name = org_id_to_schema(auth.org_id)
    accepted_jobs: list[tuple[SynthesisJob, object, SynthesisUploadItem]] = []
    failed: list[SynthesisBatchFailure] = []

    for item in req.uploads:
        file = repo.get_file_by_id(db, item.file_id)
        if file is None:
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="파일을 찾을 수 없습니다",
                )
            )
            continue

        if file.status != "UPLOADED":
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="업로드가 완료되지 않은 파일입니다",
                )
            )
            continue

        if req.project_id is not None and (
            file.owner_type != "project" or file.owner_id != req.project_id
        ):
            failed.append(
                SynthesisBatchFailure(
                    file_id=item.file_id,
                    reason="해당 프로젝트에 속하지 않은 파일입니다",
                )
            )
            continue

        job = repo.create_synthesis_job(
            db=db,
            mapping_id=record.id,
            file_id=file.id,
        )
        accepted_jobs.append((job, file, item))

    # 5. SynthesisBatch 생성
    batch = repo.create_synthesis_batch(
        db=db,
        batch_id=generate_uuid7(),
        project_id=req.project_id,
        mapping_id=record.id,
        requested_count=len(req.uploads),
        accepted_count=len(accepted_jobs),
        failed_uploads=[f.model_dump(mode="json") for f in failed],
    )

    # 6. 성공 job들에 batch_id 할당
    for job, _file, _item in accepted_jobs:
        job.assign_batch(batch.id)

    # 7. usage_count 일괄 증가
    if accepted_jobs:
        repo.increment_mapping_usage(db, record, revision, len(accepted_jobs))

    db.flush()
    db.refresh(batch)
    for job, _file, _item in accepted_jobs:
        db.refresh(job)

    # 8. 각 job에 대해 background task 등록
    for job, file, item in accepted_jobs:
        add_background_task(
            guarded(run_synthesis),
            job_id=job.id,
            schema_name=schema_name,
            graph_name=schema_name,
            file_key=file.file_key,
            filename=file.original_name,
            sheet_name=revision.sheet_name,
            mapping_json=revision.mapping,
            root_context=item.root_context,
            overwrite=req.overwrite,
        )

    logger.info(
        "합성 시작: batch_id={batch_id} mapping_id={mapping_id} accepted={accepted} failed={failed}",
        batch_id=batch.id,
        mapping_id=record.id,
        accepted=len(accepted_jobs),
        failed=len(failed),
    )
    # 9. 항상 배치 응답 반환
    return SynthesisBatchStartResponse(
        batch_id=batch.id,
        requested_count=batch.requested_count,
        accepted_count=batch.accepted_count,
        items=[to_job_response(job) for job, _file, _item in accepted_jobs],
        failed=failed,
    )
