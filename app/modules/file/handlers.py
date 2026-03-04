"""File 도메인 이벤트 핸들러.

다른 Aggregate에서 발생한 이벤트를 구독하여 File 상태를 변경한다.
같은 트랜잭션 내에서 실행되므로 비즈니스 로직과 함께 commit/rollback 된다.
"""

from app.core.event_bus import event_bus
from app.core.exceptions import AppError
from app.core.transactional import get_active_session
from app.modules.file.events import FileAttached, FileDetached
from app.modules.file.models import File
from app.modules.organization import repository as org_repo


def _on_file_attached(event: FileAttached) -> None:
    """파일들에 소유자 할당 + 스토리지 소비."""
    db = get_active_session()
    total_bytes = 0
    for file_id in event.file_ids:
        file = db.get(File, file_id)
        if file is not None:
            file.assign_owner(event.owner_type, event.owner_id)
            total_bytes += file.file_size
    if total_bytes > 0:
        if not org_repo.consume_storage_bytes(db, event.org_id, total_bytes):
            raise AppError(
                message="스토리지 한도를 초과했습니다. 플랜을 업그레이드해주세요.",
                code="QUOTA_EXCEEDED",
            )


def _on_file_detached(event: FileDetached) -> None:
    """소유자에서 분리된 파일을 소프트 삭제 + 스토리지 반환."""
    db = get_active_session()
    file = db.get(File, event.file_id)
    if file is not None:
        file_size = file.file_size
        file.soft_delete()
        if file_size > 0:
            org_repo.release_storage_bytes(db, event.org_id, file_size)


event_bus.subscribe(FileAttached, _on_file_attached)
event_bus.subscribe(FileDetached, _on_file_detached)
