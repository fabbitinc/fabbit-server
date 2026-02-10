"""도면 분석 도메인 ORM 모델.

테넌트 스키마에 생성되는 도면 분석 레코드 및 합성 작업 테이블입니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class DrawingAnalysisRecord(TenantBase):
    __tablename__ = "drawing_analysis_records"

    __table_args__ = (
        # 업로드별 분석 레코드 조회 최적화
        Index("ix_drawing_analysis_records_upload_id", "upload_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    upload_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("uploads.id", ondelete="CASCADE"),
        nullable=False,
    )
    name: Mapped[str] = mapped_column(String(500), nullable=False)
    analysis: Mapped[dict] = mapped_column(JSONB, nullable=False)
    page_count: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class DrawingSynthesisJob(TenantBase):
    __tablename__ = "drawing_synthesis_jobs"

    __table_args__ = (
        # 분석 레코드별 합성 작업 조회 최적화
        Index("ix_drawing_synthesis_jobs_analysis_id", "analysis_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    analysis_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("drawing_analysis_records.id", ondelete="CASCADE"),
        nullable=False,
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="PENDING"
    )
    nodes_created: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    relationships_created: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )
    errors: Mapped[list] = mapped_column(JSONB, nullable=False, default=list)
    started_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    completed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
