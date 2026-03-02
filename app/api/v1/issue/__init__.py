from fastapi import APIRouter

from app.api.v1.issue.change_router import router as change_router
from app.api.v1.issue.issue_router import router as issue_router

router = APIRouter()

router.include_router(issue_router)
router.include_router(change_router)
