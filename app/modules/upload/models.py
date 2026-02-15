"""업로드 도메인 ORM 모델.

테넌트 스키마에 생성되는 파일 업로드 메타데이터 테이블입니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import BigInteger, DateTime, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class Upload(TenantBase):
    __tablename__ = "uploads"

    __table_args__ = (
        # 소유자별 파일 조회 최적화 (owner_type + owner_id 복합)
        Index("ix_uploads_owner", "owner_type", "owner_id"),
        # 파일 키 유일성 보장
        UniqueConstraint("file_key", name="uq_uploads_file_key"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    original_name: Mapped[str] = mapped_column(String(500), nullable=False)
    file_key: Mapped[str] = mapped_column(String(1000), nullable=False)
    content_type: Mapped[str] = mapped_column(String(100), nullable=False)
    file_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="PENDING"
    )
    # 다형성 소유권: "project", "folder", "part", "supplier" 등
    owner_type: Mapped[str | None] = mapped_column(String(50))
    owner_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
