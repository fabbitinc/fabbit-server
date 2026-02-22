"""업로드 도메인 ORM 모델.

테넌트 스키마에 생성되는 파일 업로드 메타데이터 테이블입니다.
"""

import uuid
from datetime import datetime, timezone

from sqlalchemy import BigInteger, DateTime, Index, String, UniqueConstraint
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column
from sqlalchemy.sql import func

from app.core.database import TenantBase, generate_uuid7
from app.modules.upload.constants import ConversionStatus, UploadStatus


class Upload(TenantBase):
    __tablename__ = "uploads"

    __table_args__ = (
        # 소유자별 파일 조회 최적화 (owner_type + owner_id 복합)
        Index("ix_uploads_owner_type_owner_id", "owner_type", "owner_id"),
        # 파일 키 유일성 보장
        UniqueConstraint("file_key", name="uq_uploads_file_key"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    original_name: Mapped[str] = mapped_column(String(500), nullable=False)
    file_key: Mapped[str] = mapped_column(String(1000), nullable=False)
    content_type: Mapped[str] = mapped_column(String(100), nullable=False)
    file_size: Mapped[int] = mapped_column(BigInteger, nullable=False)
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default=UploadStatus.PENDING
    )
    # 다형성 소유권: "project", "folder", "part", "supplier" 등
    owner_type: Mapped[str | None] = mapped_column(String(50), nullable=True)
    owner_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True
    )
    # DWG → PDF/썸네일 변환 상태 (None: 비대상)
    conversion_status: Mapped[str | None] = mapped_column(
        String(20), nullable=True
    )
    pdf_key: Mapped[str | None] = mapped_column(String(500), nullable=True)
    thumbnail_key: Mapped[str | None] = mapped_column(String(500), nullable=True)

    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
    deleted_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )

    def mark_uploaded(self) -> None:
        """S3 업로드 확인 완료."""
        self.status = UploadStatus.UPLOADED

    def mark_deleted(self) -> None:
        """소프트 삭제 처리."""
        self.status = UploadStatus.DELETED
        self.deleted_at = datetime.now(timezone.utc)

    def mark_expired(self) -> None:
        """stale 업로드 만료 처리."""
        self.status = UploadStatus.EXPIRED

    def request_conversion(self) -> None:
        """DWG 변환 요청 상태 설정."""
        self.conversion_status = ConversionStatus.PENDING

    def fail_conversion(self) -> None:
        """DWG 변환 실패 상태 설정."""
        self.conversion_status = ConversionStatus.FAILED

    def complete_conversion(self, pdf_key: str | None, thumbnail_key: str | None) -> None:
        """DWG 변환 완료 결과 반영."""
        self.conversion_status = ConversionStatus.COMPLETED
        self.pdf_key = pdf_key
        self.thumbnail_key = thumbnail_key
