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

# ── TODO: 프론트 확인 후 삭제 ──
_BATCH_MOCKS: list[dict] = [
    # MOCK[0] PENDING — 시작 직후, 모든 작업 대기
    {
        "batch_id": "019505a1-0000-7000-8000-000000000001",
        "requested_count": 3,
        "accepted_count": 3,
        "failed_count": 0,
        "pending_count": 3,
        "processing_count": 0,
        "completed_count": 0,
        "failed_job_count": 0,
        "status": "PENDING",
        "failed": [],
        "items": [
            {
                "job_id": "019505a1-1000-7000-8000-000000000001",
                "upload_id": "019505a1-2000-7000-8000-000000000001",
                "status": "PENDING",
                "total_rows": 0,
                "processed_rows": 0,
                "nodes_created": 0,
                "relationships_created": 0,
                "error_count": 0,
                "started_at": None,
                "completed_at": None,
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000002",
                "upload_id": "019505a1-2000-7000-8000-000000000002",
                "status": "PENDING",
                "total_rows": 0,
                "processed_rows": 0,
                "nodes_created": 0,
                "relationships_created": 0,
                "error_count": 0,
                "started_at": None,
                "completed_at": None,
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000003",
                "upload_id": "019505a1-2000-7000-8000-000000000003",
                "status": "PENDING",
                "total_rows": 0,
                "processed_rows": 0,
                "nodes_created": 0,
                "relationships_created": 0,
                "error_count": 0,
                "started_at": None,
                "completed_at": None,
            },
        ],
        "created_at": "2026-02-19T10:00:00Z",
    },
    # MOCK[1] PROCESSING — 1개 완료 + 1개 처리 중(42%) + 1개 대기
    {
        "batch_id": "019505a1-0000-7000-8000-000000000001",
        "requested_count": 3,
        "accepted_count": 3,
        "failed_count": 0,
        "pending_count": 1,
        "processing_count": 1,
        "completed_count": 1,
        "failed_job_count": 0,
        "status": "PROCESSING",
        "failed": [],
        "items": [
            {
                "job_id": "019505a1-1000-7000-8000-000000000001",
                "upload_id": "019505a1-2000-7000-8000-000000000001",
                "status": "COMPLETED",
                "total_rows": 120,
                "processed_rows": 120,
                "nodes_created": 45,
                "relationships_created": 38,
                "error_count": 0,
                "started_at": "2026-02-19T10:00:01Z",
                "completed_at": "2026-02-19T10:00:08Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000002",
                "upload_id": "019505a1-2000-7000-8000-000000000002",
                "status": "PROCESSING",
                "total_rows": 500,
                "processed_rows": 210,
                "nodes_created": 80,
                "relationships_created": 65,
                "error_count": 2,
                "started_at": "2026-02-19T10:00:08Z",
                "completed_at": None,
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000003",
                "upload_id": "019505a1-2000-7000-8000-000000000003",
                "status": "PENDING",
                "total_rows": 0,
                "processed_rows": 0,
                "nodes_created": 0,
                "relationships_created": 0,
                "error_count": 0,
                "started_at": None,
                "completed_at": None,
            },
        ],
        "created_at": "2026-02-19T10:00:00Z",
    },
    # MOCK[2] COMPLETED — 전체 성공
    {
        "batch_id": "019505a1-0000-7000-8000-000000000001",
        "requested_count": 3,
        "accepted_count": 3,
        "failed_count": 0,
        "pending_count": 0,
        "processing_count": 0,
        "completed_count": 3,
        "failed_job_count": 0,
        "status": "COMPLETED",
        "failed": [],
        "items": [
            {
                "job_id": "019505a1-1000-7000-8000-000000000001",
                "upload_id": "019505a1-2000-7000-8000-000000000001",
                "status": "COMPLETED",
                "total_rows": 120,
                "processed_rows": 120,
                "nodes_created": 45,
                "relationships_created": 38,
                "error_count": 0,
                "started_at": "2026-02-19T10:00:01Z",
                "completed_at": "2026-02-19T10:00:08Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000002",
                "upload_id": "019505a1-2000-7000-8000-000000000002",
                "status": "COMPLETED",
                "total_rows": 500,
                "processed_rows": 500,
                "nodes_created": 180,
                "relationships_created": 150,
                "error_count": 3,
                "started_at": "2026-02-19T10:00:08Z",
                "completed_at": "2026-02-19T10:00:25Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000003",
                "upload_id": "019505a1-2000-7000-8000-000000000003",
                "status": "COMPLETED",
                "total_rows": 50,
                "processed_rows": 50,
                "nodes_created": 20,
                "relationships_created": 15,
                "error_count": 0,
                "started_at": "2026-02-19T10:00:25Z",
                "completed_at": "2026-02-19T10:00:28Z",
            },
        ],
        "created_at": "2026-02-19T10:00:00Z",
    },
    # MOCK[3] COMPLETED_WITH_ERRORS — 2개 성공 + 1개 FAILED
    {
        "batch_id": "019505a1-0000-7000-8000-000000000001",
        "requested_count": 3,
        "accepted_count": 3,
        "failed_count": 0,
        "pending_count": 0,
        "processing_count": 0,
        "completed_count": 2,
        "failed_job_count": 1,
        "status": "COMPLETED_WITH_ERRORS",
        "failed": [],
        "items": [
            {
                "job_id": "019505a1-1000-7000-8000-000000000001",
                "upload_id": "019505a1-2000-7000-8000-000000000001",
                "status": "COMPLETED",
                "total_rows": 120,
                "processed_rows": 120,
                "nodes_created": 45,
                "relationships_created": 38,
                "error_count": 0,
                "started_at": "2026-02-19T10:00:01Z",
                "completed_at": "2026-02-19T10:00:08Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000002",
                "upload_id": "019505a1-2000-7000-8000-000000000002",
                "status": "FAILED",
                "total_rows": 500,
                "processed_rows": 210,
                "nodes_created": 80,
                "relationships_created": 65,
                "error_count": 1,
                "started_at": "2026-02-19T10:00:08Z",
                "completed_at": "2026-02-19T10:00:15Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000003",
                "upload_id": "019505a1-2000-7000-8000-000000000003",
                "status": "COMPLETED",
                "total_rows": 50,
                "processed_rows": 50,
                "nodes_created": 20,
                "relationships_created": 15,
                "error_count": 0,
                "started_at": "2026-02-19T10:00:15Z",
                "completed_at": "2026-02-19T10:00:18Z",
            },
        ],
        "created_at": "2026-02-19T10:00:00Z",
    },
    # MOCK[4] FAILED — 시작 시점에 모든 업로드 검증 실패
    {
        "batch_id": "019505a1-0000-7000-8000-000000000002",
        "requested_count": 2,
        "accepted_count": 0,
        "failed_count": 2,
        "pending_count": 0,
        "processing_count": 0,
        "completed_count": 0,
        "failed_job_count": 0,
        "status": "FAILED",
        "failed": [
            {
                "upload_id": "019505a1-2000-7000-8000-000000000004",
                "reason": "업로드를 찾을 수 없습니다",
            },
            {
                "upload_id": "019505a1-2000-7000-8000-000000000005",
                "reason": "업로드가 완료되지 않은 파일입니다",
            },
        ],
        "items": [],
        "created_at": "2026-02-19T11:00:00Z",
    },
    # MOCK[5] PROCESSING + 시작 실패 혼합 — 1개 시작 실패 + 1개 완료 + 1개 처리 중
    {
        "batch_id": "019505a1-0000-7000-8000-000000000003",
        "requested_count": 3,
        "accepted_count": 2,
        "failed_count": 1,
        "pending_count": 0,
        "processing_count": 1,
        "completed_count": 1,
        "failed_job_count": 0,
        "status": "PROCESSING",
        "failed": [
            {
                "upload_id": "019505a1-2000-7000-8000-000000000006",
                "reason": "해당 프로젝트에 속하지 않은 업로드입니다",
            },
        ],
        "items": [
            {
                "job_id": "019505a1-1000-7000-8000-000000000004",
                "upload_id": "019505a1-2000-7000-8000-000000000007",
                "status": "COMPLETED",
                "total_rows": 80,
                "processed_rows": 80,
                "nodes_created": 30,
                "relationships_created": 25,
                "error_count": 0,
                "started_at": "2026-02-19T12:00:01Z",
                "completed_at": "2026-02-19T12:00:06Z",
            },
            {
                "job_id": "019505a1-1000-7000-8000-000000000005",
                "upload_id": "019505a1-2000-7000-8000-000000000008",
                "status": "PROCESSING",
                "total_rows": 1000,
                "processed_rows": 500,
                "nodes_created": 200,
                "relationships_created": 180,
                "error_count": 5,
                "started_at": "2026-02-19T12:00:06Z",
                "completed_at": None,
            },
        ],
        "created_at": "2026-02-19T12:00:00Z",
    },
]


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
    # return _BATCH_MOCKS[3]
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
