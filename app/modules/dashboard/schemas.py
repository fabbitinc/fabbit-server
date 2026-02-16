"""대시보드 통계 API 스키마."""

import uuid
from datetime import datetime

from pydantic import BaseModel


class PartStats(BaseModel):
    total: int
    added_this_week: int


class BomStats(BaseModel):
    total: int


class LastSynthesis(BaseModel):
    job_id: uuid.UUID
    status: str
    completed_at: datetime | None
    nodes_created: int
    relationships_created: int


class DashboardStatsResponse(BaseModel):
    parts: PartStats
    bom_links: BomStats
    last_synthesis: LastSynthesis | None
