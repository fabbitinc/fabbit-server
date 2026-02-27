"""이슈 도메인 모델."""

import uuid
from datetime import datetime

from sqlalchemy import (
    DateTime,
    Enum,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, UpdatableMixin

from .constants import CRState, IssueState, IssueType


class Issue(AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    """이슈 — 프로젝트 내 이슈/변경요청의 공통 베이스."""

    __tablename__ = "issues"

    __table_args__ = (
        # 프로젝트 내 이슈 번호 유일성 보장
        UniqueConstraint("project_id", "number", name="uq_issues_project_id_number"),
        # 프로젝트별 이슈 조회 최적화
        Index("ix_issues_project_id", "project_id"),
    )

    __mapper_args__ = {
        "polymorphic_on": "type",
        "polymorphic_identity": IssueType.ISSUE,
    }

    project_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
        nullable=False,
    )
    number: Mapped[int] = mapped_column(Integer, nullable=False)
    type: Mapped[IssueType] = mapped_column(
        Enum(IssueType, name="issue_type"), nullable=False
    )
    title: Mapped[str] = mapped_column(String(500), nullable=False)
    body: Mapped[str | None] = mapped_column(Text, nullable=True)
    state: Mapped[IssueState] = mapped_column(
        Enum(IssueState, name="issue_state"),
        default=IssueState.OPEN,
        nullable=False,
    )
    closed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    # -- 상태 전이 메서드 --

    def close(self, now: datetime) -> None:
        """이슈를 닫는다."""
        self.state = IssueState.CLOSED
        self.closed_at = now

    def reopen(self) -> None:
        """닫힌 이슈를 다시 연다."""
        self.state = IssueState.OPEN
        self.closed_at = None


class ChangeRequest(Issue):
    """변경 요청 — Issue를 Joined Table Inheritance로 확장."""

    __tablename__ = "change_requests"

    __mapper_args__ = {
        "polymorphic_identity": IssueType.CHANGE_REQUEST,
    }

    # 부모 테이블 조인 키 (PK + FK)
    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        primary_key=True,
    )
    cr_state: Mapped[CRState] = mapped_column(
        Enum(CRState, name="cr_state"),
        default=CRState.DRAFT,
        nullable=False,
    )
    merged_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    # User id 논리적 참조 (cross-schema FK 없음)
    merged_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True
    )

    # -- 상태 전이 메서드 --

    def open_for_review(self) -> None:
        """초안을 검토 상태로 전환한다."""
        self.cr_state = CRState.OPEN

    def merge(self, now: datetime, user_id: uuid.UUID) -> None:
        """변경 요청을 반영한다."""
        self.cr_state = CRState.MERGED
        self.merged_at = now
        self.merged_by = user_id

    def close(self, now: datetime) -> None:
        """변경 요청을 닫는다 (Issue.close 오버라이드)."""
        self.cr_state = CRState.CLOSED
        super().close(now)
