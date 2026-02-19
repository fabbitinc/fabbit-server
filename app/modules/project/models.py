"""프로젝트 도메인 ORM 모델.

테넌트 스키마에 생성되는 프로젝트/폴더/프로젝트-파트 테이블입니다.
TenantBase를 상속하여 public 스키마 모델과 분리합니다.
"""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, String, Text, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7

if TYPE_CHECKING:
    from app.modules.document.models import Drawing


class Project(TenantBase):
    __tablename__ = "projects"

    __table_args__ = (
        # 프로젝트명 유일성 보장
        UniqueConstraint("name", name="uq_projects_name"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    folders: Mapped[list["Folder"]] = relationship("Folder", back_populates="project")
    drawings: Mapped[list["Drawing"]] = relationship(
        "Drawing", back_populates="project"
    )


class Folder(TenantBase):
    __tablename__ = "folders"

    __table_args__ = (
        # 프로젝트별 폴더 조회 최적화
        Index("ix_folders_project_id", "project_id"),
        # 부모 폴더별 자식 조회 최적화
        Index("ix_folders_parent_id", "parent_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    parent_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("folders.id", ondelete="CASCADE"),
        nullable=True,
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=True,
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    project: Mapped["Project | None"] = relationship(
        "Project", back_populates="folders"
    )
    parent: Mapped["Folder | None"] = relationship("Folder", remote_side="Folder.id")


class ProjectPart(TenantBase):
    """프로젝트-파트 연결 (N:M join 테이블)."""

    __tablename__ = "project_parts"

    __table_args__ = (
        # 프로젝트-파트 쌍 유일성 보장
        UniqueConstraint(
            "project_id", "part_id", name="uq_project_parts_project_id_part_id"
        ),
        # 프로젝트별 파트 조회 최적화
        Index("ix_project_parts_project_id", "project_id"),
        # 파트별 프로젝트 조회 최적화
        Index("ix_project_parts_part_id", "part_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=False,
    )
    part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
