"""프로젝트 트리 조회 데이터 접근 레이어."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.document.models import Drawing
from app.modules.project.models import Folder, Project
from app.modules.upload.models import Upload


def list_projects(db: Session) -> list[Project]:
    return db.query(Project).order_by(Project.name.asc()).all()


def list_folders(db: Session) -> list[Folder]:
    return db.query(Folder).order_by(Folder.name.asc()).all()


def get_project_upload_counts(db: Session) -> dict[uuid.UUID, int]:
    rows = (
        db.query(Upload.project_id, func.count(Upload.id))
        .filter(Upload.project_id.isnot(None))
        .group_by(Upload.project_id)
        .all()
    )
    return {
        project_id: int(count) for project_id, count in rows if project_id is not None
    }


def get_project_drawing_counts(db: Session) -> dict[uuid.UUID, int]:
    rows = (
        db.query(Drawing.project_id, func.count(Drawing.id))
        .filter(Drawing.project_id.isnot(None))
        .group_by(Drawing.project_id)
        .all()
    )
    return {
        project_id: int(count) for project_id, count in rows if project_id is not None
    }


def get_project_folder_counts(db: Session) -> dict[uuid.UUID, int]:
    rows = (
        db.query(Folder.project_id, func.count(Folder.id))
        .filter(Folder.project_id.isnot(None))
        .group_by(Folder.project_id)
        .all()
    )
    return {
        project_id: int(count) for project_id, count in rows if project_id is not None
    }


def get_folder_drawing_counts(db: Session) -> dict[uuid.UUID, int]:
    rows = (
        db.query(Drawing.folder_id, func.count(Drawing.id))
        .filter(Drawing.folder_id.isnot(None))
        .group_by(Drawing.folder_id)
        .all()
    )
    return {folder_id: int(count) for folder_id, count in rows if folder_id is not None}
