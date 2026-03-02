"""Notification API 엔드포인트."""

import uuid
from queue import Empty

from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.notification.schemas import (
    NotificationListResponse,
    UnreadCountResponse,
)
from app.modules.notification.sse_manager import sse_manager
from app.queries import notification as notification_queries
from app.use_cases import notification as notification_commands

router = APIRouter(prefix="/api/v1/notifications", tags=["notifications"])


@router.get("", response_model=NotificationListResponse)
def list_notifications(
    cursor: uuid.UUID | None = Query(None, description="이전 페이지 마지막 항목 id"),
    limit: int = Query(20, ge=1, le=50, description="조회 건수"),
    unread_only: bool = Query(False, description="미읽음만 조회"),
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """알림 목록 조회.

    cursor 기반 페이지네이션으로 최신순 알림을 조회합니다.
    `unread_only=true`로 미읽음 알림만 필터링할 수 있습니다.
    """
    return notification_queries.list_notifications(
        db, auth, cursor=cursor, limit=limit, unread_only=unread_only
    )


@router.get("/unread-count", response_model=UnreadCountResponse)
def get_unread_count(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """미읽음 알림 개수 조회."""
    return notification_queries.count_unread(db, auth)


@router.put("/{notification_id}/read", status_code=204)
def read_notification(
    notification_id: uuid.UUID,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """알림 단건 읽음 처리."""
    notification_commands.mark_as_read(db, auth, notification_id)


@router.put("/read-all", status_code=204)
def read_all_notifications(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """전체 알림 읽음 처리.

    수신자의 모든 미읽음 알림을 일괄 읽음 처리합니다.
    """
    notification_commands.mark_all_as_read(db, auth)


@router.get("/stream")
def sse_stream(
    auth: AuthContext = Depends(require_auth),
):
    """SSE 알림 스트림.

    서버에서 새 알림이 생성되면 실시간으로 이벤트를 전송합니다.
    연결 유지를 위해 30초마다 keepalive 코멘트를 전송합니다.

    **이벤트 형식:**
    - `event: connected` — 연결 성공
    - `event: notification` — 새 알림 (data에 JSON 포함)
    - `: keepalive` — 연결 유지 (SSE 코멘트)
    """
    def event_generator():
        q = sse_manager.connect(auth.user_id)
        try:
            yield "event: connected\ndata: {}\n\n"
            while True:
                try:
                    data = q.get(timeout=30)
                    yield data
                except Empty:
                    yield ": keepalive\n\n"
        except GeneratorExit:
            pass
        finally:
            sse_manager.disconnect(auth.user_id, q)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
