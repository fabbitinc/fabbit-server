"""공급사(Supplier) 도메인 ORM 모델.

테넌트 스키마에 생성되는 공급사 마스터 테이블입니다.
RDS가 SoT이며, Graph Supplier 노드에는 company_name만 유지합니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, Index, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class Supplier(TenantBase):
    __tablename__ = "suppliers"

    __table_args__ = (
        # 회사명 유일성 보장
        UniqueConstraint("company_name", name="uq_suppliers_company_name"),
        # 업체코드 검색 최적화
        Index("ix_suppliers_code", "code"),
        # 확장 속성 필터링 최적화 (GIN)
        Index(
            "ix_suppliers_extended_properties",
            "extended_properties",
            postgresql_using="gin",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    company_name: Mapped[str] = mapped_column(String(200), nullable=False)
    code: Mapped[str | None] = mapped_column(String(100), nullable=True)
    country: Mapped[str | None] = mapped_column(String(100), nullable=True)
    contact_info: Mapped[str | None] = mapped_column(Text, nullable=True)
    extended_properties: Mapped[dict] = mapped_column(
        JSONB, nullable=False, server_default="{}"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )
