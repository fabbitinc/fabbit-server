"""Part 상세 조회."""

import uuid

from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.s3_client import s3_client
from app.modules.file import repository as file_repo
from app.modules.file.constants import FileStatus
from app.modules.part import repository as repo
from app.modules.file.schemas import FileItem
from app.modules.part.schemas import (
    BomChild,
    BomParent,
    PartDetailResponse,
    RelatedDrawing,
    RelatedSupplier,
)

_s3 = s3_client


@transactional(read_only=True)
def get_part_detail(
    db: Session, auth: AuthContext, part_id: uuid.UUID
) -> PartDetailResponse:
    # 속성: RDS
    part = repo.get_by_id(db, part_id)
    if not part:
        raise AppError(message=f"Part '{part_id}'을(를) 찾을 수 없습니다", code="NOT_FOUND")

    # BOM 관계: RDS JOIN (name 포함)
    children_rows = repo.get_children(db, part.id)
    parents_rows = repo.get_parents(db, part.id)

    # Drawing/Supplier 관계: RDS
    drawing_row = repo.get_drawing(db, part.id)
    suppliers_rows = repo.get_suppliers(db, part.id)

    children = [
        BomChild(
            id=r["id"],
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in children_rows
    ]

    parents = [
        BomParent(
            id=r["id"],
            part_number=r["part_number"],
            name=r["name"],
            quantity=r["quantity"],
            extended_properties=r.get("extended_properties", {}),
        )
        for r in parents_rows
    ]

    drawing = (
        RelatedDrawing(
            id=drawing_row["id"],
            drawing_number=drawing_row["drawing_number"],
            name=drawing_row["name"],
            version=drawing_row["version"],
            status=drawing_row["status"],
            conversion_status=drawing_row["conversion_status"],
            thumbnail_url=_s3.get_file_url(drawing_row["thumbnail_key"]),
            pdf_url=_s3.get_file_url(drawing_row["pdf_key"]),
            original_file_url=_s3.get_file_url(drawing_row["original_file_key"]),
        )
        if drawing_row
        else None
    )

    suppliers = [
        RelatedSupplier(
            id=r["id"],
            company_name=r["company_name"],
            code=r["code"],
            country=r["country"],
            unit_cost=r["unit_cost"],
        )
        for r in suppliers_rows
    ]

    extended = {k: v for k, v in (part.extended_properties or {}).items() if v is not None}

    # 첨부파일: UPLOADED 상태만
    all_files = file_repo.get_files_by_owner(db, "part", part.id)
    files = [
        FileItem(
            file_id=f.id,
            original_name=f.original_name,
            content_type=f.content_type,
            file_size=f.file_size,
            file_url=_s3.get_file_url(f.file_key),
            created_at=f.created_at,
        )
        for f in all_files
        if f.status == FileStatus.UPLOADED
    ]

    return PartDetailResponse(
        id=part.id,
        part_number=part.part_number,
        name=part.name,
        revision=part.revision,
        material=part.material,
        unit=part.unit,
        description=part.description,
        category=part.category,
        lifecycle_state=part.lifecycle_state,
        is_phantom=part.is_phantom,
        lead_time_days=part.lead_time_days,
        extended_properties=extended,
        children=children,
        parents=parents,
        drawing=drawing,
        suppliers=suppliers,
        files=files,
    )
