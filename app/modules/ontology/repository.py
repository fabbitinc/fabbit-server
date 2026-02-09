"""온톨로지 데이터 접근 레이어.

AGE 그래프 Cypher 실행과 column_mappings ORM을 담당합니다.
"어떻게 저장할 것인가"만 다루고, 비즈니스 로직은 service.py에 위임합니다.
"""

import json
import re

import pandas as pd
from sqlalchemy import select, update
from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher, execute_cypher_raw
from app.modules.ontology.models import ColumnMapping


# === Cypher 값 포맷팅 ===

def escape_cypher_value(value) -> str:
    """Cypher 문자열 값 이스케이프 (injection 방지)"""
    if pd.isna(value) or value is None:
        return ""
    s = str(value).strip()
    s = s.replace("\\", "\\\\")
    s = s.replace("'", "\\'")
    s = re.sub(r"[;\-]{2,}", "", s)
    return s


def format_cypher_value(value, data_type: str = "string") -> str | None:
    """데이터 타입에 맞게 Cypher 리터럴 포맷팅

    - string  → '값' (따옴표)
    - integer → 123 (따옴표 없음)
    - float   → 12.5 (따옴표 없음)
    - boolean → true/false (따옴표 없음)
    """
    if pd.isna(value) or value is None or str(value).strip() == "":
        return None

    if data_type == "integer":
        try:
            return str(int(float(value)))
        except (ValueError, TypeError):
            return f"'{escape_cypher_value(value)}'"

    if data_type == "float":
        try:
            return str(float(value))
        except (ValueError, TypeError):
            return f"'{escape_cypher_value(value)}'"

    if data_type == "boolean":
        s = str(value).strip().lower()
        if s in ("true", "1", "yes", "y"):
            return "true"
        if s in ("false", "0", "no", "n"):
            return "false"
        return f"'{escape_cypher_value(value)}'"

    return f"'{escape_cypher_value(value)}'"


# === 노드/관계 Cypher 생성 ===

def build_merge_node_cypher(
    label: str,
    merge_keys: dict[str, str],
    set_props: dict[str, str],
    org_id: str,
) -> str:
    """노드 MERGE Cypher 생성"""
    merge_parts = [f"_org_id: '{escape_cypher_value(org_id)}'"]
    for k, v in merge_keys.items():
        merge_parts.append(f"{k}: {v}")

    merge_str = ", ".join(merge_parts)

    if set_props:
        set_parts = [f"n.{k} = {v}" for k, v in set_props.items()]
        set_str = " SET " + ", ".join(set_parts)
    else:
        set_str = ""

    return f"MERGE (n:{label} {{{merge_str}}}){set_str}"


def build_merge_relationship_cypher(
    from_label: str,
    from_keys: dict[str, str],
    to_label: str,
    to_keys: dict[str, str],
    rel_type: str,
    rel_props: dict[str, str],
    org_id: str,
) -> str:
    """관계 MERGE Cypher 생성"""
    escaped_org = escape_cypher_value(org_id)

    from_parts = [f"_org_id: '{escaped_org}'"]
    for k, v in from_keys.items():
        from_parts.append(f"{k}: {v}")
    from_str = ", ".join(from_parts)

    to_parts = [f"_org_id: '{escaped_org}'"]
    for k, v in to_keys.items():
        to_parts.append(f"{k}: {v}")
    to_str = ", ".join(to_parts)

    if rel_props:
        prop_parts = [f"{k}: {v}" for k, v in rel_props.items()]
        rel_prop_str = " {" + ", ".join(prop_parts) + "}"
    else:
        rel_prop_str = ""

    return (
        f"MATCH (a:{from_label} {{{from_str}}}), (b:{to_label} {{{to_str}}}) "
        f"MERGE (a)-[:{rel_type}{rel_prop_str}]->(b)"
    )


# === 그래프 쿼리 실행 ===

def execute_graph_query(db: Session, cypher: str) -> list:
    """Cypher 쿼리 실행 후 결과 반환"""
    return execute_cypher(db, cypher)


def execute_graph_merge(db: Session, cypher: str) -> None:
    """단일 MERGE Cypher 실행 (커밋은 호출자가 관리)"""
    execute_cypher_raw(db, cypher)



# === 매핑 저장소 (ORM) ===

def save_mapping(db: Session, org_id: str, name: str, original_headers: list[str], mapping: dict) -> ColumnMapping:
    """매핑을 DB에 저장하고 ORM 객체 반환"""
    entity = ColumnMapping(
        org_id=org_id,
        name=name,
        original_headers=original_headers,
        mapping=mapping,
    )
    db.add(entity)
    db.commit()
    db.refresh(entity)
    return entity


def get_mapping(db: Session, mapping_id: str, org_id: str) -> ColumnMapping | None:
    """ID와 org_id로 매핑 조회"""
    stmt = select(ColumnMapping).where(
        ColumnMapping.id == mapping_id,
        ColumnMapping.org_id == org_id,
    )
    return db.execute(stmt).scalar_one_or_none()


def list_mappings(db: Session, org_id: str) -> list[ColumnMapping]:
    """org_id별 매핑 목록 조회"""
    stmt = (
        select(ColumnMapping)
        .where(ColumnMapping.org_id == org_id)
        .order_by(ColumnMapping.created_at.desc())
    )
    return list(db.execute(stmt).scalars().all())


def increment_mapping_usage(db: Session, mapping_id: str) -> None:
    """매핑 사용 횟수 증가"""
    stmt = (
        update(ColumnMapping)
        .where(ColumnMapping.id == mapping_id)
        .values(usage_count=ColumnMapping.usage_count + 1)
    )
    db.execute(stmt)
    db.commit()


def get_extended_property_hints(db: Session, org_id: str) -> list[str]:
    """해당 테넌트의 확장 속성 목록을 DB에서 추출"""
    stmt = (
        select(ColumnMapping.mapping)
        .where(ColumnMapping.org_id == org_id)
        .order_by(ColumnMapping.created_at.desc())
        .limit(5)
    )
    rows = db.execute(stmt).scalars().all()

    ext_props = set()
    for mapping_data in rows:
        if isinstance(mapping_data, str):
            mapping_data = json.loads(mapping_data)
        for ep in mapping_data.get("extended_properties", []):
            prop_name = ep.get("property_name", "")
            source = ep.get("source_column", "")
            label = ep.get("target_label", "")
            if prop_name:
                ext_props.add(f"{label}.{prop_name} (원본: {source})")
    return sorted(ext_props)
