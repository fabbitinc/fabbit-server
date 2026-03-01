"""Part 첨부파일 목록 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.file import repository as file_repo
from app.modules.file.constants import FileStatus
from app.modules.file.mapper import to_file_item
from app.modules.part import repository as repo
from app.modules.part.schemas import PartFilesResponse


@transactional(read_only=True)
def get_part_files(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartFilesResponse:
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(
            message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND"
        )

    all_files = file_repo.get_files_by_owner(db, "part", part.id)
    items = [to_file_item(f) for f in all_files if f.status == FileStatus.UPLOADED]

    return PartFilesResponse(
        total=len(items),
        items=items,
    )
