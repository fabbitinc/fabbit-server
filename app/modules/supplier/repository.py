"""공급사(Supplier) 도메인 Repository — RDS + Graph 캡슐화."""

from sqlalchemy.orm import Session

from app.core.database import generate_uuid7
from app.infrastructure.age_client import execute_cypher_raw
from app.modules.ontology.cypher_utils import escape_cypher_value
from app.modules.supplier.models import Supplier

# Supplier 모델의 표준 속성 (온톨로지 정의 속성 중 RDS 컬럼에 매핑되는 것)
_SUPPLIER_STANDARD_ATTRS = {"code", "country", "contact_info"}


# ── Supplier CRUD ──


def upsert_supplier(
    db: Session,
    company_name: str,
    props: dict,
    graph_name: str,
    *,
    overwrite: bool = False,
) -> None:
    """Supplier를 RDS에 INSERT/UPDATE하고, Graph에 MERGE.

    RDS: 전체 속성 저장
    Graph: company_name만 유지 (merge key)

    overwrite=False: DB에 이미 값이 있는 필드는 유지 (빈 필드만 채움)
    overwrite=True: 엑셀 값으로 덮어쓰기
    """
    # ── RDS upsert ──
    standard: dict = {}
    extended: dict = {}
    for key, value in props.items():
        if key == "company_name":
            continue
        if key.startswith("_ext_"):
            extended[key] = value
        elif key in _SUPPLIER_STANDARD_ATTRS:
            standard[key] = value
        else:
            extended[key] = value

    existing = (
        db.query(Supplier).filter(Supplier.company_name == company_name).first()
    )

    if existing is None:
        supplier = Supplier(
            id=generate_uuid7(),
            company_name=company_name,
            extended_properties=extended if extended else {},
            **standard,
        )
        db.add(supplier)
        db.flush()
    else:
        changed = False
        for key, value in standard.items():
            current = getattr(existing, key)
            if not overwrite and current is not None:
                continue
            if current != value:
                setattr(existing, key, value)
                changed = True

        if extended:
            merged_ext = dict(existing.extended_properties or {})
            for key, value in extended.items():
                if not overwrite and merged_ext.get(key) is not None:
                    continue
                if merged_ext.get(key) != value:
                    merged_ext[key] = value
                    changed = True
            if changed:
                existing.extended_properties = merged_ext

        if changed:
            db.flush()

    # ── Graph MERGE (company_name만) ──
    escaped = escape_cypher_value(company_name)
    cypher = f"MERGE (n:Supplier {{company_name: '{escaped}'}})"
    execute_cypher_raw(db, cypher, graph_name)


def list_suppliers_paginated(
    db: Session,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> tuple[list[Supplier], int]:
    """Supplier 목록 페이징 조회 (RDS)."""
    query = db.query(Supplier)
    if search:
        query = query.filter(
            Supplier.company_name.ilike(f"%{search}%")
            | Supplier.code.ilike(f"%{search}%")
        )
    total = query.count()
    suppliers = (
        query.order_by(Supplier.company_name).offset(offset).limit(limit).all()
    )
    return suppliers, total


def search_merge_key(
    db: Session,
    search: str,
    limit: int = 10,
) -> list[dict]:
    """root_context 자동완성용 merge key 검색 (company_name, label=code)."""
    query = db.query(Supplier.company_name, Supplier.code).filter(
        Supplier.company_name.ilike(f"%{search}%")
    )
    rows = query.order_by(Supplier.company_name).limit(limit).all()
    return [{"value": r.company_name, "label": r.code} for r in rows]
