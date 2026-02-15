"""온톨로지 스키마 API — 정적 스키마 조회."""

from fastapi import APIRouter

from app.modules.ontology import service
from app.modules.ontology.schemas import OntologySchemaResponse

router = APIRouter(prefix="/api/v1/ontology", tags=["ontology"])


@router.get("/schema", response_model=OntologySchemaResponse)
def get_ontology_schema():
    """온톨로지 스키마 조회 (정적 데이터, 인증 불필요)."""
    return service.get_ontology_schema()
