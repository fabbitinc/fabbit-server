"""라벨 도메인 모델."""

from sqlalchemy import String, UniqueConstraint
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, UpdatableMixin


class Label(AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    """라벨 — 이슈/변경요청을 분류하기 위한 태그."""

    __tablename__ = "labels"

    __table_args__ = (
        # 테넌트 내 라벨 이름 유일성 보장
        UniqueConstraint("name", name="uq_labels_name"),
    )

    name: Mapped[str] = mapped_column(String(50), nullable=False)
    description: Mapped[str | None] = mapped_column(String(200), nullable=True)
    color: Mapped[str] = mapped_column(String(7), nullable=False)
