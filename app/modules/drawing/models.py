"""도면(Drawing) 도메인 ORM 모델.

테넌트 스키마에 생성되는 도면 테이블, 분석 레코드, 합성 작업 테이블입니다.
"""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, ForeignKey, Index, Integer, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7
from app.modules.drawing.constants import ConversionStatus

if TYPE_CHECKING:
    from app.modules.file.models import File
    from app.modules.project.models import Folder, Project


class Drawing(TenantBase):
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
        ForeignKey("folders.id", ondelete="SET NULL"),
        nullable=True,
    )
    project_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("projects.id", ondelete="CASCADE"),
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

    project: Mapped["Project | None"] = relationship(
        "Project", back_populates="drawings"
    )
    folder: Mapped["Folder | None"] = relationship("Folder")
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

    # ── 상태 전이 메서드 ──

    def complete_conversion(
        self,
        pdf_file_id: uuid.UUID | None,
        pdf_key: str | None,
        thumbnail_file_id: uuid.UUID | None,
        thumbnail_key: str | None,
    ) -> None:
        """DWG 변환 완료 — PDF/썸네일 파일 연결 + 반정규화 키 설정."""
        self.conversion_status = ConversionStatus.COMPLETED
        self.pdf_file_id = pdf_file_id
        self.pdf_key = pdf_key
        self.thumbnail_file_id = thumbnail_file_id
        self.thumbnail_key = thumbnail_key

    def fail_conversion(self) -> None:
        """DWG 변환 실패."""
        self.conversion_status = ConversionStatus.FAILED


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


class DrawingSynthesisJob(TenantBase):
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
    status: Mapped[str] = mapped_column(String(20), nullable=False, default="PENDING")
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
