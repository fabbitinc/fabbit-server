"""매핑 도메인 ORM 모델.

테넌트 스키마에 생성되는 매핑 레코드 테이블입니다.
LLM이 생성한 온톨로지 매핑을 사용자가 검토/확정한 결과를 저장합니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class MappingRecord(TenantBase):
    __tablename__ = "mapping_records"

    __table_args__ = (
        # 업로드별 매핑 조회 최적화
        Index("ix_mapping_records_upload_id", "upload_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    upload_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("uploads.id", ondelete="CASCADE"),
        nullable=False,
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    original_headers: Mapped[dict] = mapped_column(JSONB, nullable=False)
    mapping: Mapped[dict] = mapped_column(JSONB, nullable=False)
    usage_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
