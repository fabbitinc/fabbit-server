"""프로젝트 도메인 모델."""

from sqlalchemy import String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, UpdatableMixin


class Project(AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    __tablename__ = "projects"

    name: Mapped[str] = mapped_column(String(200), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
