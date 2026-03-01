"""프로젝트(Project) 도메인 Repository."""

import uuid

from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.modules.project.models import Project, ProjectMember, ProjectPart
from app.modules.user.models import User


def add(db: Session, entity: Project) -> Project:
    """Project 저장."""
    db.add(entity)
    db.flush()
    return entity


def search_merge_key(
    db: Session,
    search: str,
    limit: int = 10,
) -> list[dict]:
    """온톨로지 root_context 자동완성용 프로젝트명 검색."""
    query = db.query(Project.name).filter(Project.name.ilike(f"%{search}%"))
    rows = query.order_by(Project.name).limit(limit).all()
    return [{"value": r.name, "label": r.name} for r in rows]


def list_projects_paginated(
    db: Session,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[tuple[Project, int]], int]:
    """Project 목록 페이징 조회 (RDS). 각 항목은 (Project, part_count) 튜플."""
    part_count = (
        select(func.count(ProjectPart.id))
        .where(ProjectPart.project_id == Project.id)
        .correlate(Project)
        .scalar_subquery()
        .label("part_count")
    )
    query = db.query(Project, part_count)
    if search:
        query = query.filter(Project.name.ilike(f"%{search}%"))
    total = query.count()
    rows = query.order_by(Project.name).offset(offset).limit(limit).all()
    return rows, total


def get_project_by_id(db: Session, project_id: uuid.UUID) -> Project | None:
    """Project 단건 조회."""
    return db.query(Project).filter(Project.id == project_id).first()


def link_parts(db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """Project에 Part 배치 연결 — 이미 연결된 건은 무시, 신규 연결 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(ProjectPart.part_id)
        .filter(
            ProjectPart.project_id == project_id,
            ProjectPart.part_id.in_(part_ids),
        )
        .all()
    )
    new_ids = [pid for pid in part_ids if pid not in existing]
    for pid in new_ids:
        db.add(ProjectPart(project_id=project_id, part_id=pid))
    if new_ids:
        db.flush()
    return len(new_ids)


def unlink_parts(db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]) -> int:
    """Project에서 Part 배치 해제 — 삭제 건수 반환."""
    count = (
        db.query(ProjectPart)
        .filter(
            ProjectPart.project_id == project_id,
            ProjectPart.part_id.in_(part_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


def count_linked_parts(db: Session, project_id: uuid.UUID) -> int:
    """Project에 연결된 Part 수 조회."""
    return (
        db.query(func.count(ProjectPart.id))
        .filter(ProjectPart.project_id == project_id)
        .scalar()
    )


def get_linked_part_ids(db: Session, project_id: uuid.UUID) -> list[uuid.UUID]:
    """Project에 연결된 Part ID 목록 조회."""
    rows = (
        db.query(ProjectPart.part_id)
        .filter(ProjectPart.project_id == project_id)
        .all()
    )
    return [r[0] for r in rows]


def filter_unlinked_part_ids(
    db: Session, project_id: uuid.UUID, part_ids: list[uuid.UUID]
) -> list[uuid.UUID]:
    """part_ids 중 Project에 연결되지 않은 ID 목록 반환."""
    linked = set(
        row[0]
        for row in db.query(ProjectPart.part_id)
        .filter(
            ProjectPart.project_id == project_id,
            ProjectPart.part_id.in_(part_ids),
        )
        .all()
    )
    return [pid for pid in part_ids if pid not in linked]


def get_linked_project_ids(db: Session, part_id: uuid.UUID) -> list[uuid.UUID]:
    """Part가 속한 Project ID 목록 조회."""
    rows = (
        db.query(ProjectPart.project_id)
        .filter(ProjectPart.part_id == part_id)
        .all()
    )
    return [r[0] for r in rows]


# ── Project ↔ Member ──


def add_members(
    db: Session,
    project_id: uuid.UUID,
    user_ids: list[uuid.UUID],
    role: str = "MEMBER",
) -> int:
    """Project에 멤버 배치 추가 — 이미 추가된 건은 무시, 신규 추가 건수 반환."""
    existing = set(
        row[0]
        for row in db.query(ProjectMember.user_id)
        .filter(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id.in_(user_ids),
        )
        .all()
    )
    new_ids = [uid for uid in user_ids if uid not in existing]
    for uid in new_ids:
        db.add(ProjectMember(project_id=project_id, user_id=uid, role=role))
    if new_ids:
        db.flush()
    return len(new_ids)


def remove_members(db: Session, project_id: uuid.UUID, user_ids: list[uuid.UUID]) -> int:
    """Project에서 멤버 배치 제거 — 삭제 건수 반환."""
    count = (
        db.query(ProjectMember)
        .filter(
            ProjectMember.project_id == project_id,
            ProjectMember.user_id.in_(user_ids),
        )
        .delete(synchronize_session="fetch")
    )
    db.flush()
    return count


def lookup_members(
    db: Session,
    project_id: uuid.UUID,
    *,
    search: str | None = None,
    limit: int = 10,
) -> list[User]:
    """프로젝트 멤버 lookup 조회 (picker/autocomplete용)."""
    query = (
        db.query(User)
        .join(ProjectMember, ProjectMember.user_id == User.id)
        .filter(ProjectMember.project_id == project_id)
    )
    if search:
        query = query.filter(User.full_name.ilike(f"%{search}%"))
    return query.order_by(User.full_name).limit(limit).all()


def list_member_ids(db: Session, project_id: uuid.UUID) -> list[uuid.UUID]:
    """Project에 소속된 멤버 User ID 목록 조회."""
    rows = (
        db.query(ProjectMember.user_id)
        .filter(ProjectMember.project_id == project_id)
        .all()
    )
    return [r[0] for r in rows]


def list_members(
    db: Session, project_id: uuid.UUID
) -> list[ProjectMember]:
    """Project 멤버 목록 조회 (role 포함)."""
    return (
        db.query(ProjectMember)
        .filter(ProjectMember.project_id == project_id)
        .all()
    )
