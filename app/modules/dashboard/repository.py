"""대시보드 통계 Repository."""

from datetime import datetime

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.modules.part.models import BomLink, Part
from app.modules.synthesis.models import SynthesisJob


def count_parts(db: Session) -> int:
    """전체 Part 수."""
    return db.query(func.count(Part.id)).scalar() or 0


def count_parts_since(db: Session, since: datetime) -> int:
    """since 이후 생성된 Part 수."""
    return (
        db.query(func.count(Part.id)).filter(Part.created_at >= since).scalar() or 0
    )


def count_bom_links(db: Session) -> int:
    """전체 BOM 링크 수."""
    return db.query(func.count(BomLink.id)).scalar() or 0


def get_last_synthesis_job(db: Session) -> SynthesisJob | None:
    """가장 최근 완료된 합성 작업."""
    return (
        db.query(SynthesisJob)
        .order_by(SynthesisJob.completed_at.desc().nullslast())
        .limit(1)
        .first()
    )
