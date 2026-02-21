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

if TYPE_CHECKING:
    from app.modules.project.models import Folder, Project
    from app.modules.upload.models import Upload


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
        # 업로드 파일 역추적 최적화
        Index("ix_drawings_upload_id", "upload_id"),
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
    upload_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("uploads.id", ondelete="SET NULL"),
        nullable=True,
    )
    name: Mapped[str] = mapped_column(String(200), nullable=False)
    # 원본 파일 경로 (DWG, PDF 등)
    file_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 뷰어용 PDF 경로 (DWG→변환본, PDF→원본과 동일)
    pdf_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 썸네일 WebP 경로
    thumbnail_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    # 파일 변환 상태 (PENDING, COMPLETED, FAILED)
    conversion_status: Mapped[str | None] = mapped_column(String(20), nullable=True)
    # 온톨로지 merge key (업로드 시 자동 생성, 합성 시 엑셀 값 사용)
    drawing_number: Mapped[str] = mapped_column(String(100), nullable=False)
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
    upload: Mapped["Upload | None"] = relationship("Upload")


class DrawingAnalysisRecord(TenantBase):
    __tablename__ = "drawing_analysis_records"

    __table_args__ = (
        # 업로드별 분석 레코드 조회 최적화
        Index("ix_drawing_analysis_records_upload_id", "upload_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    upload_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("uploads.id", ondelete="CASCADE"),
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
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="PENDING"
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
