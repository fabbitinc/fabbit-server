"""AI 사용량 로그 ORM 모델 (public 스키마).

조직별 AI 크레딧 사용 내역을 추적합니다.
과금/구독 데이터와 동일한 public 스키마에 위치하여 단일 트랜잭션 정합성을 보장합니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, Numeric, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import Base, generate_uuid7


class AiUsageLog(Base):
    __tablename__ = "ai_usage_logs"

    __table_args__ = (
        # 조직별 사용량 집계 최적화
        Index("ix_ai_usage_logs_org_id", "org_id"),
        # 사용자별 사용량 조회 최적화
        Index("ix_ai_usage_logs_user_id", "user_id"),
        # 기간별 조회 (조직 + 생성일)
        Index("ix_ai_usage_logs_org_id_created_at", "org_id", "created_at"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    org_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("organizations.id", ondelete="CASCADE"),
        nullable=False,
    )
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    )
    feature: Mapped[str] = mapped_column(String(50), nullable=False)
    model: Mapped[str] = mapped_column(String(50), nullable=False)
    input_tokens: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    output_tokens: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    credits_used: Mapped[float] = mapped_column(
        Numeric(10, 4), nullable=False, default=0
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
