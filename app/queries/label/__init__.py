"""Label 쿼리 — 읽기 전용 조회 re-export."""

from app.queries.label.list_labels import list_labels
from app.queries.label.lookup_labels import lookup_labels

__all__ = [
    "list_labels",
    "lookup_labels",
]
