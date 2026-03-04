"""구독 ORM 모델 (public 스키마)."""

from __future__ import annotations

import uuid
from datetime import datetime, timezone

from dateutil.relativedelta import relativedelta
from sqlalchemy import BigInteger, Boolean, DateTime, ForeignKey, Index, Integer, String, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import Base
from app.core.mixins import PkMixin, UpdatableMixin
from app.modules.organization.constants import PLAN_LIMITS, PlanType
from app.modules.subscription.constants import BillingCycle, SubscriptionStatus

class Subscription(UpdatableMixin, PkMixin, Base):
    """빌링 기간 단위 구독. ACTIVE가 현재, EXPIRED가 이력."""

    __tablename__ = "subscriptions"

    __table_args__ = (
        # org_id FK 인덱스
        Index("ix_subscriptions_org_id", "org_id"),
        # 조직당 ACTIVE 구독 유일성 (partial unique index)
        Index(
            "uq_subscriptions_org_id_active",
            "org_id",
            unique=True,
            postgresql_where=text("status = 'ACTIVE'"),
        ),
    )

    # ── FK ──

    org_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("organizations.id", ondelete="CASCADE"),
        nullable=False,
    )

    # ── 플랜/상태 ──

    plan_type: Mapped[str] = mapped_column(String(20), nullable=False)
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=SubscriptionStatus.ACTIVE
    )
    billing_cycle: Mapped[str] = mapped_column(
        String(20), nullable=False, default=BillingCycle.MONTHLY
    )

    # ── 빌링 기간 ──

    current_period_start: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )
    current_period_end: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False
    )

    # ── 한도 스냅샷 (기간 시작 시 PLAN_LIMITS에서 복사) ──

    max_members: Mapped[int] = mapped_column(Integer, nullable=False)
    ai_credits_granted: Mapped[int] = mapped_column(Integer, nullable=False)
    storage_bytes_limit: Mapped[int] = mapped_column(BigInteger, nullable=False)

    # ── 빌링 플래그 ──

    cancel_at_period_end: Mapped[bool] = mapped_column(
        Boolean, nullable=False, default=False
    )

    # ── 팩토리 메서드 ──

    @classmethod
    def create_initial(
        cls,
        org_id: uuid.UUID,
        plan_type: str,
        billing_cycle: str = BillingCycle.MONTHLY,
    ) -> Subscription:
        """조직 생성 시 초기 구독 생성. PLAN_LIMITS에서 한도를 스냅샷."""
        limits = PLAN_LIMITS[PlanType(plan_type)]
        now = datetime.now(timezone.utc)

        if billing_cycle == BillingCycle.YEARLY:
            period_end = now + relativedelta(years=1)
        else:
            period_end = now + relativedelta(months=1)

        return cls(
            org_id=org_id,
            plan_type=plan_type,
            status=SubscriptionStatus.ACTIVE,
            billing_cycle=billing_cycle,
            current_period_start=now,
            current_period_end=period_end,
            max_members=limits.max_members,
            ai_credits_granted=limits.ai_credits,
            storage_bytes_limit=limits.storage_bytes,
        )

    # ── 상태 전이 메서드 ──

    def expire(self) -> None:
        """빌링 기간 종료 → 이력 전환."""
        self.status = SubscriptionStatus.EXPIRED

    def cancel(self) -> None:
        """구독 취소 (기간 끝까지 사용 가능)."""
        self.status = SubscriptionStatus.CANCELED

    def mark_past_due(self) -> None:
        """결제 실패 유예 상태로 전환."""
        self.status = SubscriptionStatus.PAST_DUE
