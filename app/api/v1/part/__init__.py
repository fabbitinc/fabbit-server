from fastapi import APIRouter

from app.api.v1.part.part_assignee_router import router as assignee_router
from app.api.v1.part.part_router import router as part_router
from app.api.v1.part.part_team_assignment_router import (
    router as team_assignment_router,
)

router = APIRouter()

router.include_router(part_router)
router.include_router(assignee_router)
router.include_router(team_assignment_router)
