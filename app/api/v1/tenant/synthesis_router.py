"""합성 API 라우터."""

import uuid

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.synthesis.schemas import (
    SynthesisBatchStartResponse,
    SynthesisBatchStatusResponse,
    SynthesisJobResponse,
    SynthesisListResponse,
    SynthesisStartRequest,
)
from app.queries import synthesis as synthesis_queries
from app.use_cases import synthesis as synthesis_commands

router = APIRouter(prefix="/api/v1/synthesis", tags=["synthesis"])


@router.post("", response_model=SynthesisBatchStartResponse)
def start_synthesis(
    req: SynthesisStartRequest,
    background_tasks: BackgroundTasks,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """매핑 기반 합성 배치를 시작한다.

    업로드 파일 목록을 받아 각각에 대해 백그라운드 합성 작업을 등록하고,
    배치 정보를 즉시 반환한다. 실제 합성(S3 다운로드 → Excel 파싱 → Part/BOM 적재)은
    응답 이후 백그라운드에서 진행된다.

    - **scope 검증**: 매핑의 scope(PART_LIST, FULL_BOM, ROOT_BOM)에 따라
      `root_context` 필수/금지 여부가 결정된다.
    - **overwrite**: `false`(기본값)이면 기존 데이터 유지하고 빈 필드만 채움.
      `true`이면 엑셀 데이터로 덮어쓰기.
    - 진행 상황은 `GET /synthesis/batches/{batch_id}`로 폴링.
    """
    return synthesis_commands.start_synthesis(db, auth, req, background_tasks.add_task)


@router.get("/batches/{batch_id}", response_model=SynthesisBatchStatusResponse)
def get_synthesis_batch(
    batch_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    """배치 진행 상황을 조회한다.

    배치에 포함된 모든 작업의 상태를 집계하여 반환한다.
    프론트엔드에서 주기적으로 폴링하여 진행률을 표시하는 데 사용한다.

    - **status**: `PENDING` → `PROCESSING` → `COMPLETED` / `COMPLETED_WITH_ERRORS` / `FAILED`
    - 개별 작업별 처리 행 수, 생성된 노드/관계 수, 에러 목록 포함.
    """
    return synthesis_queries.get_synthesis_batch(db, batch_id)


@router.get("/{job_id}", response_model=SynthesisJobResponse)
def get_synthesis_job(
    job_id: uuid.UUID,
    db: Session = Depends(get_tenant_db),
):
    """개별 합성 작업 상세를 조회한다.

    배치 내 특정 업로드 파일에 대한 합성 작업의 상태, 처리 행 수,
    생성된 노드/관계 수, 에러 목록 등을 반환한다.
    """
    return synthesis_queries.get_synthesis_job(db, job_id)


@router.get("", response_model=SynthesisListResponse)
def list_synthesis_jobs(
    db: Session = Depends(get_tenant_db),
):
    """전체 합성 작업 이력을 조회한다.

    테넌트 내 모든 합성 작업을 최신순으로 반환한다.
    합성 이력 관리 화면에서 사용한다.
    """
    return synthesis_queries.list_synthesis_jobs(db)
