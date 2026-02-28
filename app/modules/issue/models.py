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
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase
from app.core.mixins import AuditMixin, PkMixin, TimestampMixin, UpdatableMixin

from typing import TYPE_CHECKING

from app.core.exceptions import AppError
from app.modules.file.events import FileAttached, FileDetached

from .constants import CRState, IssueState, IssueType
from .events import CRStateChanged, IssueStateChanged

if TYPE_CHECKING:
    from app.modules.file.models import File


class Issue(AggregateRoot, AuditMixin, UpdatableMixin, PkMixin, TenantBase):
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

    # ── Relationships ──

    # 다형성 소유권 기반 (File.owner_type='issue', File.owner_id=Issue.id)
    # soft-deleted 파일은 SoftDeleteMixin auto-filter가 자동 제외
    files: Mapped[list["File"]] = relationship(
        "File",
        primaryjoin=(
            "and_(Issue.id == foreign(File.owner_id),"
            " File.owner_type == 'issue')"
        ),
        viewonly=True,
    )

    # ── 파일 연결/분리 ──

    def attach_files(self, files: list["File"]) -> None:
        """검증된 파일들을 Issue에 연결 — 소유자 할당은 FileHandler가 처리."""
        self.register_event(
            FileAttached(
                owner_type="issue", owner_id=self.id, file_ids=[f.id for f in files]
            )
        )

    def detach_file(self, file_id: uuid.UUID) -> None:
        """Issue 첨부파일 1건 분리 — 소프트 삭제는 FileHandler가 처리."""
        target = next((f for f in self.files if f.id == file_id), None)
        if target is None:
            raise AppError(
                message=f"Issue '{self.id}'에 연결된 파일 '{file_id}'을(를) 찾을 수 없습니다",
                code="NOT_FOUND",
            )
        self.register_event(
            FileDetached(owner_type="issue", owner_id=self.id, file_id=file_id)
        )

    # -- 상태 전이 메서드 --

    def close(self, now: datetime) -> None:
        """이슈를 닫는다."""
        old_state = self.state.value
        self.state = IssueState.CLOSED
        self.closed_at = now
        self.register_event(IssueStateChanged(
            project_id=self.project_id,
            issue_id=self.id,
            number=self.number,
            title=self.title,
            old_state=old_state,
            new_state=IssueState.CLOSED.value,
        ))

    def reopen(self) -> None:
        """닫힌 이슈를 다시 연다."""
        old_state = self.state.value
        self.state = IssueState.OPEN
        self.closed_at = None
        self.register_event(IssueStateChanged(
            project_id=self.project_id,
            issue_id=self.id,
            number=self.number,
            title=self.title,
            old_state=old_state,
            new_state=IssueState.OPEN.value,
        ))


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
        old_state = self.cr_state.value
        self.cr_state = CRState.OPEN
        self.register_event(CRStateChanged(
            project_id=self.project_id,
            issue_id=self.id,
            number=self.number,
            title=self.title,
            old_state=old_state,
            new_state=CRState.OPEN.value,
        ))

    def merge(self, now: datetime, user_id: uuid.UUID) -> None:
        """변경 요청을 반영한다."""
        old_state = self.cr_state.value
        self.cr_state = CRState.MERGED
        self.merged_at = now
        self.merged_by = user_id
        self.register_event(CRStateChanged(
            project_id=self.project_id,
            issue_id=self.id,
            number=self.number,
            title=self.title,
            old_state=old_state,
            new_state=CRState.MERGED.value,
        ))

    def close(self, now: datetime) -> None:
        """변경 요청을 닫는다 (Issue.close 오버라이드)."""
        old_state = self.cr_state.value
        self.cr_state = CRState.CLOSED
        self.register_event(CRStateChanged(
            project_id=self.project_id,
            issue_id=self.id,
            number=self.number,
            title=self.title,
            old_state=old_state,
            new_state=CRState.CLOSED.value,
        ))
        super().close(now)


class IssueAssignee(TimestampMixin, PkMixin, TenantBase):
    """이슈 담당자 (M:N)."""

    __tablename__ = "issue_assignees"

    __table_args__ = (
        # 동일 이슈-사용자 관계 중복 방지
        UniqueConstraint("issue_id", "user_id", name="uq_issue_assignees_issue_id_user_id"),
        # 이슈 기준 담당자 조회 최적화
        Index("ix_issue_assignees_issue_id", "issue_id"),
        # 사용자 기준 담당 이슈 조회 최적화
        Index("ix_issue_assignees_user_id", "user_id"),
    )

    issue_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        nullable=False,
    )
    # User id 논리적 참조 (cross-schema FK 없음)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        nullable=False,
    )


class IssueComment(AuditMixin, UpdatableMixin, PkMixin, TenantBase):
    """이슈 댓글."""

    __tablename__ = "issue_comments"

    __table_args__ = (
        # 이슈별 댓글 조회 최적화
        Index("ix_issue_comments_issue_id", "issue_id"),
    )

    issue_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        nullable=False,
    )
    body: Mapped[str] = mapped_column(Text, nullable=False)


class IssuePart(TimestampMixin, PkMixin, TenantBase):
    """이슈 ↔ 부품 연결 (M:N)."""

    __tablename__ = "issue_parts"

    __table_args__ = (
        # 동일 이슈-부품 관계 중복 방지
        UniqueConstraint("issue_id", "part_id", name="uq_issue_parts_issue_id_part_id"),
        # 이슈 기준 부품 조회 최적화
        Index("ix_issue_parts_issue_id", "issue_id"),
        # 부품 기준 이슈 조회 최적화 (역추적)
        Index("ix_issue_parts_part_id", "part_id"),
    )

    issue_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        nullable=False,
    )
    part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )


class IssueLabel(TimestampMixin, PkMixin, TenantBase):
    """이슈 ↔ 라벨 연결 (M:N)."""

    __tablename__ = "issue_labels"

    __table_args__ = (
        # 동일 이슈-라벨 관계 중복 방지
        UniqueConstraint("issue_id", "label_id", name="uq_issue_labels_issue_id_label_id"),
        # 이슈 기준 라벨 조회 최적화
        Index("ix_issue_labels_issue_id", "issue_id"),
        # 라벨 기준 이슈 조회 최적화 (역추적)
        Index("ix_issue_labels_label_id", "label_id"),
    )

    issue_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        nullable=False,
    )
    label_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("labels.id", ondelete="CASCADE"),
        nullable=False,
    )


class ChangeRequestIssue(TimestampMixin, PkMixin, TenantBase):
    """변경 요청 ↔ 이슈 연결 (M:N)."""

    __tablename__ = "change_request_issues"

    __table_args__ = (
        # 동일 CR-이슈 관계 중복 방지
        UniqueConstraint(
            "change_request_id", "issue_id",
            name="uq_change_request_issues_cr_id_issue_id",
        ),
        # CR 기준 연결 이슈 조회 최적화
        Index("ix_change_request_issues_change_request_id", "change_request_id"),
        # 이슈 기준 연결 CR 조회 최적화 (역추적)
        Index("ix_change_request_issues_issue_id", "issue_id"),
    )

    change_request_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("change_requests.id", ondelete="CASCADE"),
        nullable=False,
    )
    issue_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("issues.id", ondelete="CASCADE"),
        nullable=False,
    )
