"""활성화 및 탐색 도메인 데이터 접근 레이어."""

import json

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.mapping.models import MappingRecord


def count_nodes_by_labels(
    db: Session, graph_name: str, labels: list[str]
) -> dict[str, int]:
    counts: dict[str, int] = {}
    for label in labels:
        try:
            rows = execute_cypher(db, f"MATCH (n:{label}) RETURN count(n)", graph_name)
            counts[label] = rows[0] if rows else 0
        except Exception:
            counts[label] = 0
    return counts


def count_relationships_by_types(
    db: Session,
    graph_name: str,
    rel_types: list[str],
) -> dict[str, int]:
    counts: dict[str, int] = {}
    for rel_type in rel_types:
        try:
            rows = execute_cypher(
                db,
                f"MATCH ()-[r:{rel_type}]->() RETURN count(r)",
                graph_name,
            )
            counts[rel_type] = rows[0] if rows else 0
        except Exception:
            counts[rel_type] = 0
    return counts


def count_orphan_parts(db: Session, graph_name: str, total_parts: int) -> int:
    connected = 0
    try:
        rows = execute_cypher(
            db,
            "MATCH ()-[:CONSISTS_OF]->(p:Part) RETURN count(DISTINCT p)",
            graph_name,
        )
        connected += rows[0] if rows else 0
    except Exception:
        db.rollback()

    try:
        rows = execute_cypher(
            db,
            "MATCH ()-[:HAS_ITEM]->(p:Part) RETURN count(DISTINCT p)",
            graph_name,
        )
        connected += rows[0] if rows else 0
    except Exception:
        db.rollback()

    return max(0, total_parts - connected)


def count_parts_without_drawing(db: Session, graph_name: str, total_parts: int) -> int:
    try:
        rows = execute_cypher(
            db,
            "MATCH (p:Part)-[:DEFINED_BY]->() RETURN count(DISTINCT p)",
            graph_name,
        )
        with_drawing = rows[0] if rows else 0
        return max(0, total_parts - with_drawing)
    except Exception:
        db.rollback()
        return 0


def count_parts_without_supplier(db: Session, graph_name: str, total_parts: int) -> int:
    try:
        rows = execute_cypher(
            db,
            "MATCH (p:Part)-[:SUPPLIED_BY]->() RETURN count(DISTINCT p)",
            graph_name,
        )
        with_supplier = rows[0] if rows else 0
        return max(0, total_parts - with_supplier)
    except Exception:
        db.rollback()
        return 0


def count_leaf_parts_without_bom(db: Session, graph_name: str) -> int:
    try:
        rows = execute_cypher(
            db,
            "MATCH (parent:Part)-[:CONSISTS_OF]->(child:Part) "
            "WHERE child.name IS NULL "
            "RETURN count(child)",
            graph_name,
        )
        return rows[0] if rows else 0
    except Exception:
        db.rollback()
        return 0


def execute_graph_query(db: Session, cypher: str, graph_name: str) -> list:
    return execute_cypher(db, cypher, graph_name)


def list_extended_hints(db: Session) -> list[str]:
    try:
        records = (
            db.query(MappingRecord.mapping)
            .order_by(MappingRecord.created_at.desc())
            .limit(5)
            .all()
        )
        ext_props: set[str] = set()
        for (mapping_data,) in records:
            if isinstance(mapping_data, str):
                mapping_data = json.loads(mapping_data)
            for ep in mapping_data.get("extended_properties", []):
                prop_name = ep.get("property_name", "")
                source = ep.get("source_column", "")
                label = ep.get("target_label", "")
                if prop_name:
                    ext_props.add(f"{label}.{prop_name} (원본: {source})")
        return sorted(ext_props)
    except Exception:
        return []
