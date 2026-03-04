"""사용자 ORM 모델 (public 스키마)."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import Boolean, DateTime, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.aggregate import AggregateRoot
from app.core.database import Base, generate_uuid7
from app.modules.file.events import FileAttached, FileDetached

if TYPE_CHECKING:
    from app.modules.file.models import File
    from app.modules.organization.models import Membership


class User(AggregateRoot, Base):
    __tablename__ = "users"

    __table_args__ = (
        # 이메일 유일성
        UniqueConstraint("email", name="uq_users_email"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(100), nullable=False)
    phone: Mapped[str | None] = mapped_column(String(20), nullable=True)
    profile_image_file_key: Mapped[str | None] = mapped_column(
        String(1000), nullable=True
    )
    is_active: Mapped[bool] = mapped_column(Boolean, nullable=False, default=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    memberships: Mapped[list["Membership"]] = relationship(
        "Membership", back_populates="user"
    )

    # ── 도메인 메서드 ──

    def set_profile_image(self, file: "File", org_id: uuid.UUID) -> None:
        """프로필 이미지 설정 — 소유자 할당은 FileHandler가 처리."""
        self.profile_image_file_key = file.file_key
        self.register_event(
            FileAttached(org_id=org_id, owner_type="user", owner_id=self.id, file_ids=[file.id])
        )

    def remove_profile_image(self, file_id: uuid.UUID, org_id: uuid.UUID) -> None:
        """프로필 이미지 제거 — 소프트 삭제는 FileHandler가 처리."""
        self.profile_image_file_key = None
        self.register_event(
            FileDetached(org_id=org_id, owner_type="user", owner_id=self.id, file_id=file_id)
        )
