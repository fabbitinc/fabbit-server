"""온보딩 완료 처리."""

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.transactional import transactional
from app.modules.organization import service as org_service
from app.modules.organization.schemas import OrganizationResponse


@transactional()
def complete_onboarding(
    db: Session,
    auth: AuthContext,
) -> OrganizationResponse:
    """조직 온보딩 완료."""
    return org_service.complete_onboarding(db, auth)
