from fastapi import APIRouter

from app.api.v1.organization.org_invitation_router import router as invitation_router
from app.api.v1.organization.org_router import router as org_router

router = APIRouter()

router.include_router(org_router)
router.include_router(invitation_router)
