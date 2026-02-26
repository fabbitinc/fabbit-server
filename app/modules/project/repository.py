"""프로젝트(Project) 도메인 Repository."""

import uuid

from sqlalchemy.orm import Session

from app.modules.project.models import Project


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
) -> tuple[list[Project], int]:
    """Project 목록 페이징 조회 (RDS)."""
    query = db.query(Project)
    if search:
        query = query.filter(Project.name.ilike(f"%{search}%"))
    total = query.count()
    projects = query.order_by(Project.name).offset(offset).limit(limit).all()
    return projects, total


def get_project_by_id(db: Session, project_id: uuid.UUID) -> Project | None:
    """Project 단건 조회."""
    return db.query(Project).filter(Project.id == project_id).first()
