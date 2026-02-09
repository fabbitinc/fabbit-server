"""온톨로지 데이터 접근 레이어.

AGE 그래프 Cypher 실행과 column_mappings SQL을 담당합니다.
"어떻게 저장할 것인가"만 다루고, 비즈니스 로직은 service.py에 위임합니다.
"""

import json
import re

import pandas as pd

from app.infrastructure.age_client import (
    GRAPH,
    create_connection,
    execute_cypher,
    execute_sql,
)


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

def execute_graph_query(cypher: str) -> list:
    """Cypher 쿼리 실행 후 결과 반환"""
    return execute_cypher(cypher)


def execute_graph_merge(cypher: str, conn=None):
    """단일 MERGE Cypher 실행 (배치 커넥션 사용 가능)"""
    if conn is None:
        conn = create_connection()
    with conn.cursor() as cur:
        sql = f"SELECT * FROM cypher('{GRAPH}', $$ {cypher} $$) AS (result agtype);"
        cur.execute(sql)


def create_batch_connection():
    """배치 인제스션용 새 커넥션 생성"""
    return create_connection()


# === 매핑 저장소 (column_mappings 테이블) ===

def save_mapping(org_id: str, name: str, original_headers: list[str], mapping: dict) -> dict:
    """매핑을 DB에 저장하고 결과 반환"""
    rows = execute_sql(
        """
        INSERT INTO column_mappings (org_id, name, original_headers, mapping)
        VALUES (%s, %s, %s, %s)
        RETURNING id, org_id, name, created_at
        """,
        (
            org_id,
            name,
            json.dumps(original_headers, ensure_ascii=False),
            json.dumps(mapping, ensure_ascii=False),
        ),
    )
    return rows[0] if rows else None


def get_mapping(mapping_id: str, org_id: str) -> dict | None:
    """ID와 org_id로 매핑 조회"""
    rows = execute_sql(
        "SELECT mapping, usage_count FROM column_mappings WHERE id = %s AND org_id = %s",
        (mapping_id, org_id),
    )
    return rows[0] if rows else None


def list_mappings(org_id: str) -> list[dict]:
    """org_id별 매핑 목록 조회"""
    return execute_sql(
        """
        SELECT id, org_id, name, original_headers, mapping, usage_count, created_at
        FROM column_mappings
        WHERE org_id = %s
        ORDER BY created_at DESC
        """,
        (org_id,),
    )


def increment_mapping_usage(mapping_id: str):
    """매핑 사용 횟수 증가"""
    execute_sql(
        "UPDATE column_mappings SET usage_count = usage_count + 1 WHERE id = %s",
        (mapping_id,),
    )


def get_extended_property_hints(org_id: str) -> list[str]:
    """해당 테넌트의 확장 속성 목록을 DB에서 추출"""
    rows = execute_sql(
        "SELECT mapping FROM column_mappings WHERE org_id = %s ORDER BY created_at DESC LIMIT 5",
        (org_id,),
    )
    ext_props = set()
    for row in rows:
        mapping_data = row["mapping"]
        if isinstance(mapping_data, str):
            mapping_data = json.loads(mapping_data)
        for ep in mapping_data.get("extended_properties", []):
            prop_name = ep.get("property_name", "")
            source = ep.get("source_column", "")
            label = ep.get("target_label", "")
            if prop_name:
                ext_props.add(f"{label}.{prop_name} (원본: {source})")
    return sorted(ext_props)
