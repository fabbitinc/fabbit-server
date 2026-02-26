"""Project 쿼리 — 읽기 전용 조회 함수 re-export."""

from app.queries.project.get_project_detail import get_project_detail
from app.queries.project.list_projects import list_projects

__all__ = [
    "get_project_detail",
    "list_projects",
]
