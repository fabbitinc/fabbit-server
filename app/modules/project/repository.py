"""프로젝트 도메인 데이터 접근 레이어."""

import uuid

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.document.models import Drawing
from app.modules.part.models import Part
from app.modules.project.models import Folder, Project, ProjectPart
from app.modules.upload.models import Upload


# ── 트리 조회 (기존) ──


def list_projects(db: Session) -> list[Project]:
    return db.query(Project).order_by(Project.name.asc()).all()


def list_folders(db: Session) -> list[Folder]:
    return db.query(Folder).order_by(Folder.name.asc()).all()


def get_project_upload_counts(db: Session) -> dict[uuid.UUID, int]:
    """프로젝트별 업로드 수 (다형성 owner_type 기반)."""
    rows = (
        db.query(Upload.owner_id, func.count(Upload.id))
        .filter(Upload.owner_type == "project", Upload.owner_id.isnot(None))
        .group_by(Upload.owner_id)
        .all()
    )
    return {owner_id: int(count) for owner_id, count in rows if owner_id is not None}


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


# ── Project CRUD ──


def get_project_by_id(db: Session, project_id: uuid.UUID) -> Project | None:
    return db.query(Project).filter(Project.id == project_id).first()


def create_project(
    db: Session,
    name: str,
    description: str | None,
) -> Project:
    project = Project(name=name, description=description)
    db.add(project)
    db.flush()
    return project


def update_project(
    db: Session,
    project: Project,
    name: str | None,
    description: str | None,
) -> Project:
    if name is not None:
        project.name = name
    if description is not None:
        project.description = description
    db.flush()
    return project


def delete_project(db: Session, project_id: uuid.UUID) -> None:
    db.query(Project).filter(Project.id == project_id).delete()


# ── Folder CRUD ──


def get_folder_by_id(db: Session, folder_id: uuid.UUID) -> Folder | None:
    return db.query(Folder).filter(Folder.id == folder_id).first()


def create_folder(
    db: Session,
    name: str,
    project_id: uuid.UUID,
    parent_id: uuid.UUID | None,
) -> Folder:
    folder = Folder(name=name, project_id=project_id, parent_id=parent_id)
    db.add(folder)
    db.flush()
    return folder


def update_folder(db: Session, folder: Folder, name: str | None) -> Folder:
    if name is not None:
        folder.name = name
    db.flush()
    return folder


def move_folder(db: Session, folder: Folder, parent_id: uuid.UUID | None) -> Folder:
    folder.parent_id = parent_id
    db.flush()
    return folder


def delete_folder(db: Session, folder_id: uuid.UUID) -> None:
    db.query(Folder).filter(Folder.id == folder_id).delete()


def get_folder_ids_by_project(db: Session, project_id: uuid.UUID) -> list[uuid.UUID]:
    """프로젝트에 속한 모든 폴더 ID 목록."""
    rows = db.query(Folder.id).filter(Folder.project_id == project_id).all()
    return [row[0] for row in rows]


def get_descendant_folder_ids(db: Session, folder_id: uuid.UUID) -> list[uuid.UUID]:
    """재귀적으로 하위 폴더 ID를 수집 (BFS)."""
    result: list[uuid.UUID] = []
    queue = [folder_id]
    while queue:
        current = queue.pop(0)
        children = (
            db.query(Folder.id).filter(Folder.parent_id == current).all()
        )
        for (child_id,) in children:
            result.append(child_id)
            queue.append(child_id)
    return result


# ── ProjectPart (프로젝트-파트 연결) ──


def add_part_to_project(
    db: Session,
    project_id: uuid.UUID,
    part_id: uuid.UUID,
) -> ProjectPart:
    pp = ProjectPart(project_id=project_id, part_id=part_id)
    db.add(pp)
    db.flush()
    return pp


def remove_part_from_project(
    db: Session,
    project_id: uuid.UUID,
    part_id: uuid.UUID,
) -> None:
    db.query(ProjectPart).filter(
        ProjectPart.project_id == project_id,
        ProjectPart.part_id == part_id,
    ).delete()


def get_project_parts(db: Session, project_id: uuid.UUID) -> list[Part]:
    return (
        db.query(Part)
        .join(ProjectPart, ProjectPart.part_id == Part.id)
        .filter(ProjectPart.project_id == project_id)
        .order_by(Part.part_number)
        .all()
    )


def get_project_part_count(db: Session, project_id: uuid.UUID) -> int:
    return (
        db.query(func.count(ProjectPart.id))
        .filter(ProjectPart.project_id == project_id)
        .scalar()
        or 0
    )
