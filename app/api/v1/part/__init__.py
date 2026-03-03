from fastapi import APIRouter

from app.api.v1.part.part_owner_default_router import (
    router as owner_default_router,
)
from app.api.v1.part.part_owner_router import router as owner_router
from app.api.v1.part.part_router import router as part_router

router = APIRouter()

router.include_router(part_router)
router.include_router(owner_router)
router.include_router(owner_default_router)
