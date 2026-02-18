"""온톨로지 스키마 API — 정적 스키마 조회."""

from fastapi import APIRouter, Depends

from app.api.deps import require_auth
from app.core.auth_context import AuthContext
from app.modules.ontology import service
from app.modules.ontology.schemas import OntologySchemaResponse

router = APIRouter(prefix="/api/v1/ontology", tags=["ontology"])


@router.get("/schema", response_model=OntologySchemaResponse)
def get_ontology_schema(_auth: AuthContext = Depends(require_auth)):
    """온톨로지 스키마 조회."""
    return service.get_ontology_schema()
