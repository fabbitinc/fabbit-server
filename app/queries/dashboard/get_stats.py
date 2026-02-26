"""대시보드 통계 조회."""

from datetime import UTC, datetime, timedelta

from sqlalchemy.orm import Session

from app.core.transactional import transactional
from app.modules.dashboard import repository as repo
from app.modules.dashboard.schemas import (
    BomStats,
    DashboardStatsResponse,
    LastSynthesis,
    PartStats,
)


@transactional(read_only=True)
def get_stats(db: Session) -> DashboardStatsResponse:
    """대시보드 통계 조회."""
    since = datetime.now(UTC) - timedelta(days=7)

    total_parts = repo.count_parts(db)
    added_this_week = repo.count_parts_since(db, since)
    total_bom_links = repo.count_bom_links(db)
    last_job = repo.get_last_synthesis_job(db)

    last_synthesis = None
    if last_job:
        last_synthesis = LastSynthesis(
            job_id=last_job.id,
            status=last_job.status,
            completed_at=last_job.completed_at,
            nodes_created=last_job.nodes_created,
            relationships_created=last_job.relationships_created,
        )

    return DashboardStatsResponse(
        parts=PartStats(total=total_parts, added_this_week=added_this_week),
        bom_links=BomStats(total=total_bom_links),
        last_synthesis=last_synthesis,
    )
