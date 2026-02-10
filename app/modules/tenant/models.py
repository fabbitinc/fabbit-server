"""테넌트 스키마 ORM 모델.

tenant_{org_id} 스키마에 생성되는 비즈니스 테이블입니다.
TenantBase를 상속하여 public 스키마 모델과 분리합니다.

프로비저닝 시 search_path를 전환한 뒤
TenantBase.metadata.create_all()로 한 번에 생성됩니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String, Text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class TenantProject(TenantBase):
    __tablename__ = "projects"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    folders: Mapped[list["TenantFolder"]] = relationship(
        "TenantFolder", back_populates="project"
    )
    drawings: Mapped[list["TenantDrawing"]] = relationship(
        "TenantDrawing", back_populates="project"
    )


class TenantFolder(TenantBase):
    __tablename__ = "folders"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    parent_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("folders.id", ondelete="CASCADE"),
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    project: Mapped["TenantProject | None"] = relationship(
        "TenantProject", back_populates="folders"
    )
    parent: Mapped["TenantFolder | None"] = relationship(
        "TenantFolder", remote_side="TenantFolder.id"
    )


class TenantDrawing(TenantBase):
    __tablename__ = "drawings"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    file_key: Mapped[str | None] = mapped_column(String(500))
    folder_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("folders.id", ondelete="SET NULL"),
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    project: Mapped["TenantProject | None"] = relationship(
        "TenantProject", back_populates="drawings"
    )
    folder: Mapped["TenantFolder | None"] = relationship("TenantFolder")
