"""활성화 및 탐색 도메인 데이터 접근 레이어."""

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.mapping.models import MappingRecord


def count_nodes_by_label(db: Session, graph_name: str, label: str) -> int:
    """단일 라벨의 노드 수 조회."""
    rows = execute_cypher(db, f"MATCH (n:{label}) RETURN count(n)", graph_name)
    return rows[0] if rows else 0


def count_relationships_by_type(
    db: Session,
    graph_name: str,
    rel_type: str,
) -> int:
    """단일 관계 타입의 수 조회."""
    rows = execute_cypher(
        db,
        f"MATCH ()-[r:{rel_type}]->() RETURN count(r)",
        graph_name,
    )
    return rows[0] if rows else 0


def count_orphan_parts(db: Session, graph_name: str, total_parts: int) -> int:
    connected = 0
    rows = execute_cypher(
        db,
        "MATCH ()-[:CONSISTS_OF]->(p:Part) RETURN count(DISTINCT p)",
        graph_name,
    )
    connected += rows[0] if rows else 0

    rows = execute_cypher(
        db,
        "MATCH ()-[:HAS_ITEM]->(p:Part) RETURN count(DISTINCT p)",
        graph_name,
    )
    connected += rows[0] if rows else 0

    return max(0, total_parts - connected)


def count_parts_without_drawing(db: Session, graph_name: str, total_parts: int) -> int:
    rows = execute_cypher(
        db,
        "MATCH (p:Part)-[:DEFINED_BY]->() RETURN count(DISTINCT p)",
        graph_name,
    )
    with_drawing = rows[0] if rows else 0
    return max(0, total_parts - with_drawing)


def count_parts_without_supplier(db: Session, graph_name: str, total_parts: int) -> int:
    rows = execute_cypher(
        db,
        "MATCH (p:Part)-[:SUPPLIED_BY]->() RETURN count(DISTINCT p)",
        graph_name,
    )
    with_supplier = rows[0] if rows else 0
    return max(0, total_parts - with_supplier)


def count_leaf_parts_without_bom(db: Session, graph_name: str) -> int:
    rows = execute_cypher(
        db,
        "MATCH (parent:Part)-[:CONSISTS_OF]->(child:Part) "
        "WHERE child.name IS NULL "
        "RETURN count(child)",
        graph_name,
    )
    return rows[0] if rows else 0


def execute_graph_query(db: Session, cypher: str, graph_name: str) -> list:
    return execute_cypher(db, cypher, graph_name)


def list_recent_mappings(db: Session, limit: int = 5) -> list[dict]:
    """최근 매핑 레코드의 mapping JSON 목록 반환."""
    records = (
        db.query(MappingRecord.mapping)
        .order_by(MappingRecord.created_at.desc())
        .limit(limit)
        .all()
    )
    return [row[0] for row in records]
