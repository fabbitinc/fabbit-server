from fastapi import APIRouter

from app.api.v1.tenant.member.member_invitation_router import (
    router as invitation_router,
)
from app.api.v1.tenant.member.member_router import router as member_router

router = APIRouter()

router.include_router(member_router)
router.include_router(invitation_router)
