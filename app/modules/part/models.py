"""부품(Part) 도메인 ORM 모델.

테넌트 스키마에 생성되는 부품 마스터 테이블입니다.
SoT(Single Source of Truth) 역할을 하며, Graph Part 노드에는 part_number만 유지합니다.
"""

import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import (
    Boolean,
    DateTime,
    Float,
    ForeignKey,
    Index,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy.sql import func

from app.core.aggregate import AggregateRoot
from app.core.database import TenantBase, generate_uuid7
from app.core.exceptions import AppError
from app.modules.file.events import FileAttached, FileDetached

if TYPE_CHECKING:
    from app.modules.file.models import File

# Part 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
# revision은 별도 관리하므로 제외
_STANDARD_ATTRS = {
    "name",
    "material",
    "unit",
    "description",
    "category",
    "is_phantom",
    "lifecycle_state",
    "lead_time_days",
}


class Part(AggregateRoot, TenantBase):
    __tablename__ = "parts"

    __table_args__ = (
        # 품번 유일성 보장
        UniqueConstraint("part_number", name="uq_parts_part_number"),
        # 품명 검색 최적화
        Index("ix_parts_name", "name"),
        # 분류별 조회 최적화
        Index("ix_parts_category", "category"),
        # 도면별 부품 조회 최적화
        Index("ix_parts_drawing_id", "drawing_id"),
        # 확장 속성 필터링 최적화 (GIN)
        Index(
            "ix_parts_extended_properties",
            "extended_properties",
            postgresql_using="gin",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    drawing_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("drawings.id", ondelete="SET NULL"),
        nullable=True,
    )
    part_number: Mapped[str] = mapped_column(String(100), nullable=False)
    name: Mapped[str | None] = mapped_column(String(500), nullable=True)
    revision: Mapped[str] = mapped_column(
        String(50), nullable=False, server_default="1"
    )
    material: Mapped[str | None] = mapped_column(String(200), nullable=True)
    unit: Mapped[str | None] = mapped_column(String(20), nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    category: Mapped[str | None] = mapped_column(String(100), nullable=True)
    is_phantom: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    lifecycle_state: Mapped[str | None] = mapped_column(String(50), nullable=True)
    lead_time_days: Mapped[int | None] = mapped_column(Integer, nullable=True)
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

    # ── Relationships ──

    # 다형성 소유권 기반 (File.owner_type='part', File.owner_id=Part.id)
    # 삭제된 파일 제외
    files: Mapped[list["File"]] = relationship(
        "File",
        primaryjoin=(
            "and_(Part.id == foreign(File.owner_id),"
            " File.owner_type == 'part',"
            " File.status != 'DELETED')"
        ),
        viewonly=True,
    )

    # ── 팩토리 메서드 ──

    @classmethod
    def create(cls, part_number: str, **props) -> "Part":
        """Part 신규 생성 팩토리."""
        part = cls(part_number=part_number)
        # id는 default로 생성되지만, 명시적으로 전달된 경우 사용
        if "id" in props:
            part.id = props["id"]
        # revision (별도 관리)
        if "revision" in props and props["revision"] is not None:
            part.revision = props["revision"]
        # 표준 속성 설정
        for attr in _STANDARD_ATTRS:
            if attr in props and props[attr] is not None:
                setattr(part, attr, props[attr])
        # 확장 속성 설정
        if "extended_properties" in props:
            part.extended_properties = props["extended_properties"]
        return part

    # ── 도메인 메서드 ──

    def update_properties(self, props: dict, *, overwrite: bool = False) -> list[str]:
        """표준+확장 속성을 갱신하고 변경된 필드명 목록을 반환."""
        changed: list[str] = []
        for attr in _STANDARD_ATTRS:
            if attr not in props:
                continue
            current = getattr(self, attr)
            new_val = props[attr]
            if not overwrite and current is not None:
                continue
            if current != new_val:
                setattr(self, attr, new_val)
                changed.append(attr)
        # 확장 속성
        ext = props.get("extended_properties")
        if ext:
            merged = dict(self.extended_properties or {})
            for k, v in ext.items():
                if not overwrite and merged.get(k) is not None:
                    continue
                if merged.get(k) != v:
                    merged[k] = v
                    changed.append(k)
            if changed:
                self.extended_properties = merged
        return changed

    def assign_drawing(self, drawing_id: uuid.UUID) -> None:
        """도면 연결."""
        self.drawing_id = drawing_id

    def unassign_drawing(self) -> None:
        """도면 연결 해제."""
        self.drawing_id = None

    def attach_files(self, files: list["File"]) -> None:
        """검증된 파일들을 Part에 연결 — 소유자 할당은 FileHandler가 처리."""
        self.register_event(
            FileAttached(
                owner_type="part", owner_id=self.id, file_ids=[f.id for f in files]
            )
        )

    def detach_file(self, file_id: uuid.UUID) -> None:
        """Part 첨부파일 1건 분리 — 소프트 삭제는 FileHandler가 처리."""
        target = next((f for f in self.files if f.id == file_id), None)
        if target is None:
            raise AppError(
                message=f"Part '{self.id}'에 연결된 파일 '{file_id}'을(를) 찾을 수 없습니다",
                code="NOT_FOUND",
            )
        self.register_event(
            FileDetached(owner_type="part", owner_id=self.id, file_id=file_id)
        )


class PartRevision(TenantBase):
    __tablename__ = "part_revisions"

    __table_args__ = (
        # 동일 부품의 리비전 번호 중복 방지
        UniqueConstraint(
            "part_id", "revision", name="uq_part_revisions_part_id_revision"
        ),
        # 부품별 리비전 조회 최적화
        Index("ix_part_revisions_part_id", "part_id"),
        # 합성 작업별 리비전 조회 최적화
        Index("ix_part_revisions_synthesis_job_id", "synthesis_job_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
    synthesis_job_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("synthesis_jobs.id", ondelete="SET NULL"),
        nullable=True,
    )
    # Part 전체 컬럼 스냅샷
    drawing_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), nullable=True
    )
    part_number: Mapped[str] = mapped_column(String(100), nullable=False)
    name: Mapped[str | None] = mapped_column(String(500), nullable=True)
    revision: Mapped[str] = mapped_column(String(50), nullable=False)
    material: Mapped[str | None] = mapped_column(String(200), nullable=True)
    unit: Mapped[str | None] = mapped_column(String(20), nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    category: Mapped[str | None] = mapped_column(String(100), nullable=True)
    is_phantom: Mapped[bool | None] = mapped_column(Boolean, nullable=True)
    lifecycle_state: Mapped[str | None] = mapped_column(String(50), nullable=True)
    lead_time_days: Mapped[int | None] = mapped_column(Integer, nullable=True)
    extended_properties: Mapped[dict] = mapped_column(
        JSONB, nullable=False, server_default="{}"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class BomLink(TenantBase):
    """BOM 관계 (Part → Part CONSISTS_OF)."""

    __tablename__ = "bom_links"

    __table_args__ = (
        # 동일 부모-자식 관계 중복 방지
        UniqueConstraint(
            "parent_part_id",
            "child_part_id",
            name="uq_bom_links_parent_part_id_child_part_id",
        ),
        # 부모 기준 자식 조회 최적화
        Index("ix_bom_links_parent_part_id", "parent_part_id"),
        # 자식 기준 부모 조회 최적화 (역추적)
        Index("ix_bom_links_child_part_id", "child_part_id"),
        # 확장 속성 필터링 최적화 (GIN)
        Index(
            "ix_bom_links_extended_properties",
            "extended_properties",
            postgresql_using="gin",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    parent_part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
    child_part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
    quantity: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    # sequence, reference_designator, find_number는 extended_properties로 관리
    extended_properties: Mapped[dict] = mapped_column(
        JSONB, nullable=False, server_default="{}"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class PartSupplier(TenantBase):
    """Part → Supplier 공급 관계 (M:N)."""

    __tablename__ = "part_suppliers"

    __table_args__ = (
        # 동일 Part-Supplier 관계 중복 방지
        UniqueConstraint(
            "part_id",
            "supplier_id",
            name="uq_part_suppliers_part_id_supplier_id",
        ),
        # Part 기준 공급사 조회 최적화
        Index("ix_part_suppliers_part_id", "part_id"),
        # Supplier 기준 Part 조회 최적화 (역추적)
        Index("ix_part_suppliers_supplier_id", "supplier_id"),
        # 확장 속성 필터링 최적화 (GIN)
        Index(
            "ix_part_suppliers_extended_properties",
            "extended_properties",
            postgresql_using="gin",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    part_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("parts.id", ondelete="CASCADE"),
        nullable=False,
    )
    supplier_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        ForeignKey("suppliers.id", ondelete="CASCADE"),
        nullable=False,
    )
    unit_cost: Mapped[float | None] = mapped_column(Float, nullable=True)
    extended_properties: Mapped[dict] = mapped_column(
        JSONB, nullable=False, server_default="{}"
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )


class ExtendedPropertyDefinition(TenantBase):
    """확장 속성 메타데이터 레지스트리.

    테넌트별 확장 속성의 키, 표시명, 타입을 관리합니다.
    합성(synthesis) 시 자동 등록되며, 프론트엔드 동적 필터 UI 생성에 사용됩니다.
    """

    __tablename__ = "extended_property_definitions"

    __table_args__ = (
        # 동일 엔티티에 같은 키 중복 방지
        UniqueConstraint(
            "key",
            "target_entity",
            name="uq_extended_property_definitions_key_target_entity",
        ),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=generate_uuid7
    )
    # JSONB 내부 키 (예: _ext_carbon_emission)
    key: Mapped[str] = mapped_column(String(200), nullable=False)
    # UI 표시명 (예: 탄소배출량)
    display_name: Mapped[str] = mapped_column(String(200), nullable=False)
    # 값 타입: string / integer / float / boolean
    data_type: Mapped[str] = mapped_column(String(20), nullable=False, default="string")
    # 소속 엔티티: Part / BomLink / Drawing / Supplier
    target_entity: Mapped[str] = mapped_column(String(50), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), server_default=func.now(), nullable=False
    )
