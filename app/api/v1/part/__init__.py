from fastapi import APIRouter

from app.api.v1.part.part_category_default_router import (
    router as category_default_router,
)
from app.api.v1.part.part_router import router as part_router

router = APIRouter()

router.include_router(part_router)
router.include_router(category_default_router)
