"""매핑 도메인 ORM 모델.

MappingRecord(identity) + MappingRevision(versioned content)으로 분리하여
매핑 업데이트 이력을 추적합니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, ForeignKey, Index, Integer, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7
from app.modules.mapping.constants import MappingScope


class MappingRecord(TenantBase):
    __tablename__ = "mapping_records"

    __table_args__ = (
        # 매핑 이름 유일성 보장 (비활성 포함)
        Index("uq_mapping_records_name", "name", unique=True),
        # 스코프 + 활성 상태 필터링 최적화
        Index("ix_mapping_records_scope_is_active", "scope", "is_active"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    scope: Mapped[str] = mapped_column(
        String(20), nullable=False, default=MappingScope.PART_LIST
    )
    # 활성화/비활성화 (soft-delete)
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    # 전 리비전 합산 사용 횟수
    usage_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True, onupdate=func.now()
    )

    revisions: Mapped[list["MappingRevision"]] = relationship(
        "MappingRevision", back_populates="record", order_by="MappingRevision.version"
    )


class MappingRevision(TenantBase):
    __tablename__ = "mapping_revisions"

    __table_args__ = (
        # record별 리비전 조회 최적화
        Index("ix_mapping_revisions_record_id", "record_id"),
        # record 내 버전 유일성 보장
        Index(
            "uq_mapping_revisions_record_version",
            "record_id",
            "version",
            unique=True,
        ),
        # 파일별 리비전 조회 최적화
        Index("ix_mapping_revisions_file_id", "file_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    record_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("mapping_records.id", ondelete="CASCADE"),
        nullable=False,
    )
    file_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="CASCADE"),
        nullable=False,
    )
    # record 내 auto-increment 버전
    version: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    sheet_name: Mapped[str | None] = mapped_column(String(200), nullable=True)
    original_headers: Mapped[dict] = mapped_column(JSONB, nullable=False)
    mapping: Mapped[dict] = mapped_column(JSONB, nullable=False)
    # 리비전별 사용 횟수
    usage_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    record: Mapped["MappingRecord"] = relationship(
        "MappingRecord", back_populates="revisions"
    )
