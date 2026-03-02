from fastapi import APIRouter, Depends

from app.api.deps import guard_archived_project
from app.api.v1.project.project_label_router import router as label_router
from app.api.v1.project.project_member_router import router as member_router
from app.api.v1.project.project_part_router import router as part_router
from app.api.v1.project.project_router import router as project_router

router = APIRouter()

router.include_router(project_router)
router.include_router(label_router, dependencies=[Depends(guard_archived_project)])
router.include_router(member_router, dependencies=[Depends(guard_archived_project)])
router.include_router(part_router, dependencies=[Depends(guard_archived_project)])
