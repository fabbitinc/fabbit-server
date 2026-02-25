"""합성(Synthesis) 도메인 ORM 모델.

테넌트 스키마에 생성되는 합성 작업 테이블입니다.
매핑을 기반으로 업로드 파일 데이터를 AGE 그래프에 적재하는 작업을 추적합니다.
"""

import uuid
from datetime import datetime, timezone

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase, generate_uuid7
from app.modules.synthesis.constants import SynthesisJobStatus
from app.modules.synthesis.events import (
    SynthesisJobCompleted,
    SynthesisJobFailed,
    SynthesisJobStarted,
)


class SynthesisJob(AggregateRoot, TenantBase):
    __tablename__ = "synthesis_jobs"

    __table_args__ = (
        # 배치별 합성 작업 조회 최적화
        Index("ix_synthesis_jobs_batch_id", "batch_id"),
        # 매핑별 합성 작업 조회 최적화
        Index("ix_synthesis_jobs_mapping_id", "mapping_id"),
        # 파일별 합성 작업 조회 최적화
        Index("ix_synthesis_jobs_file_id", "file_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    batch_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("synthesis_batches.id", ondelete="SET NULL"),
        nullable=True,
    )
    mapping_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("mapping_records.id", ondelete="CASCADE"),
        nullable=False,
    )
    file_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="CASCADE"),
        nullable=False,
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=SynthesisJobStatus.PENDING
    )
    total_rows: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    processed_rows: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    nodes_created: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    relationships_created: Mapped[int] = mapped_column(
        Integer, nullable=False, default=0
    )
    errors: Mapped[list] = mapped_column(JSONB, nullable=False, default=list)
    started_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    completed_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )

    # ── 팩토리 메서드 ──

    @classmethod
    def create(
        cls,
        *,
        mapping_id: uuid.UUID,
        file_id: uuid.UUID,
        batch_id: uuid.UUID | None = None,
    ) -> "SynthesisJob":
        """합성 작업 생성."""
        return cls(
            id=generate_uuid7(),
            batch_id=batch_id,
            mapping_id=mapping_id,
            file_id=file_id,
            status=SynthesisJobStatus.PENDING,
        )

    # ── 상태 전이 메서드 ──

    def assign_batch(self, batch_id: uuid.UUID) -> None:
        """배치 ID 할당."""
        self.batch_id = batch_id

    def start_processing(self) -> None:
        """합성 실행 시작."""
        self.status = SynthesisJobStatus.PROCESSING
        self.started_at = datetime.now(timezone.utc)
        self.register_event(SynthesisJobStarted(job_id=self.id))

    def set_total_rows(self, total_rows: int) -> None:
        """전체 행 수 설정."""
        self.total_rows = total_rows

    def update_progress(
        self,
        *,
        processed_rows: int,
        nodes_created: int,
        relationships_created: int,
        errors: list[str],
    ) -> None:
        """진행 상태 업데이트."""
        self.processed_rows = processed_rows
        self.nodes_created = nodes_created
        self.relationships_created = relationships_created
        self.errors = errors[:100]

    def complete(self) -> None:
        """합성 완료."""
        self.status = SynthesisJobStatus.COMPLETED
        self.completed_at = datetime.now(timezone.utc)
        self.register_event(
            SynthesisJobCompleted(
                job_id=self.id,
                nodes_created=self.nodes_created,
                relationships_created=self.relationships_created,
            )
        )

    def complete_empty(self) -> None:
        """빈 파일 즉시 완료 (데이터 없음)."""
        self.total_rows = 0
        self.status = SynthesisJobStatus.COMPLETED
        self.completed_at = datetime.now(timezone.utc)

    def fail(self, errors: list[str]) -> None:
        """합성 실패."""
        self.status = SynthesisJobStatus.FAILED
        self.errors = errors[:100]
        self.completed_at = datetime.now(timezone.utc)
        self.register_event(
            SynthesisJobFailed(job_id=self.id, errors=errors[:10])
        )


class SynthesisBatch(TenantBase):
    __tablename__ = "synthesis_batches"

    __table_args__ = (
        # 프로젝트별 배치 조회 최적화
        Index("ix_synthesis_batches_project_id", "project_id"),
        # 매핑별 배치 조회 최적화
        Index("ix_synthesis_batches_mapping_id", "mapping_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="SET NULL"),
        nullable=True,
    )
    mapping_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("mapping_records.id", ondelete="CASCADE"),
        nullable=False,
    )
    requested_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    accepted_count: Mapped[int] = mapped_column(Integer, nullable=False, default=0)
    failed_uploads: Mapped[list] = mapped_column(JSONB, nullable=False, default=list)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
