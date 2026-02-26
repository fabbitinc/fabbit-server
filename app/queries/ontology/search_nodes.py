"""노드 merge key 검색."""

import importlib

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.modules.ontology.schemas import NodeSearchItem, NodeSearchResponse

# 라벨 → repository 매핑 (lazy import 방지용 dict)
_LABEL_SEARCH_REPOS: dict[str, str] = {
    "Part": "app.modules.part.repository",
    "Drawing": "app.modules.drawing.repository",
    "Supplier": "app.modules.supplier.repository",
    "Project": "app.modules.project.repository",
}


@transactional(read_only=True)
def search_nodes(
    db: Session,
    label: str,
    search: str,
    limit: int = 10,
) -> NodeSearchResponse:
    """노드 라벨별 merge key 검색 (RDS)."""
    if label not in _LABEL_SEARCH_REPOS:
        raise AppError(
            message=f"지원하지 않는 노드 라벨입니다: {label}",
            code="INVALID_LABEL",
        )

    repo_module = importlib.import_module(_LABEL_SEARCH_REPOS[label])
    rows = repo_module.search_merge_key(db, search, limit)

    items = [NodeSearchItem(value=r["value"], label=r["label"]) for r in rows]
    return NodeSearchResponse(node_label=label, items=items)
