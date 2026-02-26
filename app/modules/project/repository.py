"""프로젝트 도메인 최소 조회 레이어."""

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
