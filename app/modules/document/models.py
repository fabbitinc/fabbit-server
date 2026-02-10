"""도면 도메인 ORM 모델.

테넌트 스키마에 생성되는 도면 테이블입니다.
TenantBase를 상속하여 public 스키마 모델과 분리합니다.
"""

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7


class Drawing(TenantBase):
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

    project: Mapped["Project | None"] = relationship("Project", back_populates="drawings")
    folder: Mapped["Folder | None"] = relationship("Folder")
