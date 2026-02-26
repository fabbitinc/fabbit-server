"""File 도메인 이벤트 핸들러.

다른 Aggregate에서 발생한 이벤트를 구독하여 File 상태를 변경한다.
같은 트랜잭션 내에서 실행되므로 비즈니스 로직과 함께 commit/rollback 된다.
"""

from app.core.event_bus import event_bus
from app.core.transactional import get_active_session
from app.modules.file.events import FileAttached, FileDetached
from app.modules.file.models import File


def _on_file_attached(event: FileAttached) -> None:
    """파일들에 소유자 할당."""
    db = get_active_session()
    for file_id in event.file_ids:
        file = db.get(File, file_id)
        if file is not None:
            file.assign_owner(event.owner_type, event.owner_id)


def _on_file_detached(event: FileDetached) -> None:
    """소유자에서 분리된 파일을 소프트 삭제 처리."""
    db = get_active_session()
    file = db.get(File, event.file_id)
    if file is not None:
        file.mark_deleted()


event_bus.subscribe(FileAttached, _on_file_attached)
event_bus.subscribe(FileDetached, _on_file_detached)
