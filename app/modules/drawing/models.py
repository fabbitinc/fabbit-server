"""도면(Drawing) 도메인 ORM 모델.

테넌트 스키마에 생성되는 도면 테이블, 분석 레코드, 합성 작업 테이블입니다.
"""

import uuid
from datetime import datetime, timezone
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase, generate_uuid7
from app.core.mixins import SoftDeleteMixin
from app.modules.drawing.constants import (
    ConversionStatus,
    DrawingSynthesisJobStatus,
)

if TYPE_CHECKING:
    from app.modules.file.models import File

# Drawing 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
_STANDARD_ATTRS = {"name", "version", "status"}


class Drawing(SoftDeleteMixin, AggregateRoot, TenantBase):
    __tablename__ = "drawings"

    __table_args__ = (
        # 도면번호 유일성 보장
        UniqueConstraint("drawing_number", name="uq_drawings_drawing_number"),
        # 도면번호 검색 최적화
        Index("ix_drawings_drawing_number", "drawing_number"),
        # 도면명 검색 최적화
        Index("ix_drawings_name", "name"),
        # 폴더별 도면 조회 최적화
        Index("ix_drawings_folder_id", "folder_id"),
        # 프로젝트별 도면 조회 최적화
        Index("ix_drawings_project_id", "project_id"),
        # 원본 파일 역추적 최적화
        Index("ix_drawings_original_file_id", "original_file_id"),
        # 변환 PDF 파일 역추적
        Index("ix_drawings_pdf_file_id", "pdf_file_id"),
        # 썸네일 파일 역추적
        Index("ix_drawings_thumbnail_file_id", "thumbnail_file_id"),
        # 확장 속성 필터링 최적화 (GIN)
        Index(
            "ix_drawings_extended_properties",
            "extended_properties",
            postgresql_using="gin",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    folder_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        nullable=True,
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        nullable=True,
    )
    original_file_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="SET NULL"),
        nullable=True,
    )
    # 변환된 PDF File 참조
    pdf_file_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="SET NULL"),
        nullable=True,
    )
    # 썸네일 File 참조
    thumbnail_file_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="SET NULL"),
        nullable=True,
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    # 원본 파일 경로 (DWG, PDF 등)
    original_file_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 뷰어용 PDF 경로 (DWG→변환본, PDF→원본과 동일)
    pdf_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 썸네일 WebP 경로
    thumbnail_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 파일 변환 상태 (PENDING, COMPLETED, FAILED)
    conversion_status: Mapped[str | None] = mapped_column(String(20), nullable=True)
    # 온톨로지 merge key (LLM 추출 또는 사용자 입력)
    drawing_number: Mapped[str | None] = mapped_column(String(100), nullable=True)
    version: Mapped[str | None] = mapped_column(String(50), nullable=True)
    status: Mapped[str | None] = mapped_column(String(50), nullable=True)
    extended_properties: Mapped[dict] = mapped_column(
        JSONB, nullable=False, server_default="{}"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    original_file: Mapped["File | None"] = relationship(
        "File", foreign_keys=[original_file_id]
    )
    pdf_file: Mapped["File | None"] = relationship("File", foreign_keys=[pdf_file_id])
    thumbnail_file: Mapped["File | None"] = relationship(
        "File", foreign_keys=[thumbnail_file_id]
    )

    # ── 팩토리 메서드 ──

    @classmethod
    def create_pending(
        cls,
        original_file_id: uuid.UUID,
        original_file_key: str,
        original_name: str,
    ) -> "Drawing":
        """DWG 업로드 완료 시 예비 Drawing 생성.

        drawing_number는 LLM 추출 또는 사용자 입력으로 설정됨.
        """
        return cls(
            id=generate_uuid7(),
            name=original_name,
            original_file_id=original_file_id,
            original_file_key=original_file_key,
            conversion_status=ConversionStatus.PENDING,
        )

    @classmethod
    def create_from_upsert(
        cls,
        *,
        drawing_number: str | None,
        original_file_id: uuid.UUID | None,
        name: str | None,
        standard_props: dict,
        extended_properties: dict,
    ) -> "Drawing":
        """합성(upsert) 경로에서 Drawing 신규 생성 팩토리."""
        return cls(
            id=generate_uuid7(),
            drawing_number=drawing_number,
            original_file_id=original_file_id,
            name=name or drawing_number or "Untitled",
            extended_properties=extended_properties or {},
            **standard_props,
        )

    # ── 상태 전이 메서드 ──

    def complete_conversion(
        self,
        pdf_file_id: uuid.UUID | None,
        pdf_key: str | None,
        thumbnail_file_id: uuid.UUID | None,
        thumbnail_key: str | None,
        org_id: uuid.UUID | None = None,
        derived_file_ids: list[uuid.UUID] | None = None,
    ) -> None:
        """DWG 변환 완료 — PDF/썸네일 파일 연결 + 반정규화 키 설정."""
        self.conversion_status = ConversionStatus.COMPLETED
        self.pdf_file_id = pdf_file_id
        self.pdf_key = pdf_key
        self.thumbnail_file_id = thumbnail_file_id
        self.thumbnail_key = thumbnail_key

        if org_id and derived_file_ids:
            from app.modules.file.events import FileAttached

            self.register_event(
                FileAttached(
                    org_id=org_id,
                    owner_type="drawing",
                    owner_id=self.id,
                    file_ids=derived_file_ids,
                )
            )

    def fail_conversion(self) -> None:
        """DWG 변환 실패."""
        self.conversion_status = ConversionStatus.FAILED

    def update_properties(
        self,
        *,
        drawing_number: str | None,
        original_file_id: uuid.UUID | None,
        standard_props: dict,
        extended_props: dict,
        overwrite: bool,
    ) -> list[str]:
        """기존 Drawing 속성 갱신 (upsert 업데이트 경로).

        Returns:
            변경된 필드명 목록.
        """
        changed: list[str] = []

        if drawing_number and self.drawing_number != drawing_number:
            self.drawing_number = drawing_number
            changed.append("drawing_number")

        if original_file_id and self.original_file_id is None:
            self.original_file_id = original_file_id
            changed.append("original_file_id")

        for key, value in standard_props.items():
            current = getattr(self, key)
            if not overwrite and current is not None:
                continue
            if current != value:
                setattr(self, key, value)
                changed.append(key)

        if extended_props:
            merged_ext = dict(self.extended_properties or {})
            for key, value in extended_props.items():
                if not overwrite and merged_ext.get(key) is not None:
                    continue
                if merged_ext.get(key) != value:
                    merged_ext[key] = value
                    changed.append(key)
            if changed:
                self.extended_properties = merged_ext

        return changed


class DrawingAnalysisRecord(TenantBase):
    __tablename__ = "drawing_analysis_records"

    __table_args__ = (
        # 파일별 분석 레코드 조회 최적화
        Index("ix_drawing_analysis_records_file_id", "file_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    file_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("files.id", ondelete="CASCADE"),
        nullable=False,
    )
    name: Mapped[str] = mapped_column(String(500), nullable=False)
    analysis: Mapped[dict] = mapped_column(JSONB, nullable=False)
    page_count: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class DrawingSynthesisJob(AggregateRoot, TenantBase):
    __tablename__ = "drawing_synthesis_jobs"

    __table_args__ = (
        # 분석 레코드별 합성 작업 조회 최적화
        Index("ix_drawing_synthesis_jobs_analysis_id", "analysis_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    analysis_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("drawing_analysis_records.id", ondelete="CASCADE"),
        nullable=False,
    )
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=DrawingSynthesisJobStatus.PENDING
    )
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
    def create(cls, *, analysis_id: uuid.UUID) -> "DrawingSynthesisJob":
        """도면 합성 작업 생성."""
        return cls(
            id=generate_uuid7(),
            analysis_id=analysis_id,
            status=DrawingSynthesisJobStatus.PENDING,
        )

    # ── 상태 전이 메서드 ──

    def start_processing(self) -> None:
        """합성 실행 시작."""
        self.status = DrawingSynthesisJobStatus.PROCESSING
        self.started_at = datetime.now(timezone.utc)

    def complete(
        self,
        *,
        nodes_created: int,
        relationships_created: int,
        errors: list[str],
    ) -> None:
        """합성 완료."""
        self.status = DrawingSynthesisJobStatus.COMPLETED
        self.nodes_created = nodes_created
        self.relationships_created = relationships_created
        self.errors = errors[:100]
        self.completed_at = datetime.now(timezone.utc)

    def fail(self, errors: list[str]) -> None:
        """합성 실패."""
        self.status = DrawingSynthesisJobStatus.FAILED
        self.errors = errors[:100]
        self.completed_at = datetime.now(timezone.utc)
