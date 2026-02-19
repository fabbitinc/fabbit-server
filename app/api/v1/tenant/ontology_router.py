"""온톨로지 스키마 API — 정적 스키마 조회 + 노드 검색."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.modules.ontology import service
from app.modules.ontology.schemas import NodeSearchResponse, OntologySchemaResponse

router = APIRouter(prefix="/api/v1/ontology", tags=["ontology"])


@router.get("/schema", response_model=OntologySchemaResponse)
def get_ontology_schema(_auth: AuthContext = Depends(require_auth)):
    """온톨로지 스키마 조회."""
    return service.get_ontology_schema()


@router.get("/nodes/search", response_model=NodeSearchResponse)
def search_nodes(
    label: str = Query(..., description="노드 라벨 (Part, Drawing, Supplier, Project)"),
    search: str = Query(..., min_length=1, description="검색어"),
    limit: int = Query(10, ge=1, le=50, description="최대 결과 수"),
    _auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """노드 라벨별 merge key 검색.

    root_context 자동완성에 사용됩니다.
    지원 라벨: Part(part_number), Drawing(drawing_number), Supplier(company_name), Project(name)
    """
    return service.search_nodes(db, label, search, limit)
