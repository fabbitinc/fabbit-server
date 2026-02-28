"""조직 초대 API 라우터."""

import uuid

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_db, require_admin
from app.core.auth_context import AuthContext
from app.modules.auth.schemas import (
    CreateInvitationRequest,
    InvitationListResponse,
    InvitationResponse,
)
from app.queries import organization as org_queries
from app.use_cases import organization as org_commands

router = APIRouter(prefix="/api/v1/organizations/invitations", tags=["organizations"])


@router.post("", response_model=InvitationResponse, status_code=201)
def create_invitation(
    req: CreateInvitationRequest,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_admin),
):
    """이메일로 조직 초대 발송.

    관리자(ADMIN)만 초대할 수 있습니다.
    이미 PENDING 상태인 초대가 있으면 중복 에러를 반환합니다.
    이전에 취소된 초대가 있으면 삭제 후 새로 생성합니다.
    """
    return org_commands.create_invitation(db, auth, req)


@router.get("", response_model=InvitationListResponse)
def list_invitations(
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_admin),
):
    """조직의 초대 목록 조회.

    관리자(ADMIN)만 조회할 수 있습니다.
    모든 상태(PENDING, ACCEPTED, CANCELLED)의 초대를 최신순으로 반환합니다.
    """
    return org_queries.list_invitations(db, auth)


@router.delete("/{invitation_id}", status_code=204)
def cancel_invitation(
    invitation_id: uuid.UUID,
    db: Session = Depends(get_db),
    auth: AuthContext = Depends(require_admin),
):
    """초대 취소.

    관리자(ADMIN)만 취소할 수 있습니다.
    PENDING 상태인 초대만 취소할 수 있습니다.
    """
    org_commands.cancel_invitation(db, auth, invitation_id)
