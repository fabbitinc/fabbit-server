"""라벨 도메인 모델."""

import uuid

from sqlalchemy import ForeignKey, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, UpdatableMixin


class Label(AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    """라벨 — 프로젝트 내 이슈/변경요청을 분류하기 위한 태그."""

    __tablename__ = "labels"

    __table_args__ = (
        # 프로젝트 내 라벨 이름 유일성 보장
        UniqueConstraint("project_id", "name", name="uq_labels_project_id_name"),
        # 프로젝트별 라벨 조회 최적화
        Index("ix_labels_project_id", "project_id"),
    )

    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=False,
    )
    name: Mapped[str] = mapped_column(String(50), nullable=False)
    description: Mapped[str | None] = mapped_column(String(200), nullable=True)
    color: Mapped[str] = mapped_column(String(7), nullable=False)
