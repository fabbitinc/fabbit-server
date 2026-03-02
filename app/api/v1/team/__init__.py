from fastapi import APIRouter

from app.api.v1.team.team_member_router import router as member_router
from app.api.v1.team.team_router import router as team_router

router = APIRouter()

router.include_router(team_router)
router.include_router(member_router)
