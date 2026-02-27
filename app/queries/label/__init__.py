"""Label 쿼리 — 읽기 전용 조회 re-export."""

from app.queries.label.list_labels import list_labels

__all__ = [
    "list_labels",
]
