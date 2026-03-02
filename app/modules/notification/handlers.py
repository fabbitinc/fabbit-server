"""Notification 이벤트 핸들러.

UserMentioned 이벤트를 구독하여, 원래 트랜잭션 커밋 후
백그라운드 워커 스레드에서 Notification 레코드를 생성하고 SSE push한다.
알림 생성 실패가 원래 요청을 롤백시키지 않는다.

리소스 제한:
- 단일 daemon 워커 스레드 + queue.Queue로 순차 처리
- DB 커넥션 1개만 사용 (워커 내부에서 재사용)
"""

import json
import queue
import threading

from loguru import logger
from sqlalchemy import event as sa_event
from sqlalchemy import text

from app.core.database import SessionLocal
from app.core.event_bus import event_bus
from app.core.transactional import get_active_session
from app.modules.issue.events import UserMentioned
from app.modules.notification.constants import NotificationType
from app.modules.notification.models import Notification
from app.modules.notification.sse_manager import sse_manager

# 알림 생성 작업 큐 (워커 스레드 1개가 순차 소비)
_task_queue: queue.Queue[tuple[str, dict]] = queue.Queue()


def _worker() -> None:
    """알림 생성 워커 — 단일 세션을 재사용하며 큐를 순차 처리."""
    db = SessionLocal()
    try:
        while True:
            search_path, kwargs = _task_queue.get()
            try:
                db.execute(text(f"SET search_path = {search_path}"))
                notification = Notification(**kwargs)
                db.add(notification)
                db.flush()
                db.commit()

                # SSE 이벤트 전송
                payload = json.dumps(
                    {
                        "id": str(notification.id),
                        "type": notification.type.value,
                        "actor_id": str(notification.actor_id),
                        "payload": notification.payload,
                    },
                    ensure_ascii=False,
                )
                sse_manager.push(
                    notification.user_id,
                    f"event: notification\ndata: {payload}\n\n",
                )
            except Exception:
                db.rollback()
                logger.warning("비동기 알림 생성 실패", exc_info=True)
            finally:
                _task_queue.task_done()
    finally:
        db.close()


# daemon 워커 스레드 — 프로세스 종료 시 자동 정리
_worker_thread = threading.Thread(target=_worker, daemon=True)
_worker_thread.start()


def _on_user_mentioned(event: UserMentioned) -> None:
    """사용자 멘션 → 커밋 후 워커 큐에 알림 생성 작업 추가."""
    db = get_active_session()
    actor_id = db.info.get("user_id")

    # 자기 자신 멘션 제외
    if actor_id and event.mentioned_user_id == actor_id:
        return

    # 현재 세션의 search_path 캡처
    search_path = db.execute(text("SHOW search_path")).scalar()

    notification_kwargs = {
        "user_id": event.mentioned_user_id,
        "type": NotificationType.MENTION,
        "actor_id": actor_id,
        "payload": {
            "source_issue_id": str(event.source_issue_id),
            "source_number": event.source_number,
            "source_title": event.source_title,
            "source_issue_type": event.source_issue_type,
            "is_comment": event.is_comment,
        },
    }

    # 커밋 후 워커 큐에 작업 추가
    def _after_commit(session):
        sa_event.remove(db, "after_commit", _after_commit)
        _task_queue.put((search_path, notification_kwargs))

    sa_event.listen(db, "after_commit", _after_commit)


# ── 구독 등록 ──

event_bus.subscribe(UserMentioned, _on_user_mentioned)
