"""Part 도메인 응답 변환 매퍼."""

from typing import Any

from app.modules.file.mapper import get_file_url
from app.modules.part.schemas import (
    BomChild,
    BomParent,
    RelatedDrawing,
    RelatedSupplier,
)


def to_bom_child(row: dict[str, Any]) -> BomChild:
    """BOM 자식 row → BomChild 변환."""
    return BomChild(
        id=row["id"],
        part_number=row["part_number"],
        name=row["name"],
        quantity=row["quantity"],
        extended_properties=row.get("extended_properties", {}),
    )


def to_bom_parent(row: dict[str, Any]) -> BomParent:
    """BOM 부모 row → BomParent 변환."""
    return BomParent(
        id=row["id"],
        part_number=row["part_number"],
        name=row["name"],
        quantity=row["quantity"],
        extended_properties=row.get("extended_properties", {}),
    )


def to_related_drawing(row: dict[str, Any] | None) -> RelatedDrawing | None:
    """Drawing row → RelatedDrawing 변환 (None-safe)."""
    if not row:
        return None
    return RelatedDrawing(
        id=row["id"],
        drawing_number=row["drawing_number"],
        name=row["name"],
        version=row["version"],
        status=row["status"],
        conversion_status=row["conversion_status"],
        thumbnail_url=get_file_url(row["thumbnail_key"]),
        pdf_url=get_file_url(row["pdf_key"]),
        original_file_url=get_file_url(row["original_file_key"]),
    )


def to_related_supplier(row: dict[str, Any]) -> RelatedSupplier:
    """Supplier row → RelatedSupplier 변환."""
    return RelatedSupplier(
        id=row["id"],
        company_name=row["company_name"],
        code=row["code"],
        country=row["country"],
        unit_cost=row["unit_cost"],
    )
