"""아이템(Part) 조회 — AGE Cypher 쿼리."""

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.ontology.repository import escape_cypher_value


def count_parts(db: Session, graph_name: str, search: str | None = None) -> int:
    """Part 전체 건수 (검색 필터 적용)"""
    if search:
        escaped = escape_cypher_value(search)
        query = (
            f"MATCH (p:Part) "
            f"WHERE p.part_number CONTAINS '{escaped}' "
            f"OR p.name CONTAINS '{escaped}' "
            f"RETURN count(p)"
        )
    else:
        query = "MATCH (p:Part) RETURN count(p)"

    rows = execute_cypher(db, query, graph_name)
    return int(rows[0]) if rows else 0


def list_parts(
    db: Session,
    graph_name: str,
    *,
    search: str | None = None,
    offset: int = 0,
    limit: int = 20,
) -> list[dict]:
    """Part 목록 + 관계 카운트 (OPTIONAL MATCH)"""
    where_clause = ""
    if search:
        escaped = escape_cypher_value(search)
        where_clause = (
            f"WHERE p.part_number CONTAINS '{escaped}' "
            f"OR p.name CONTAINS '{escaped}' "
        )

    query = (
        f"MATCH (p:Part) {where_clause}"
        f"OPTIONAL MATCH (p)-[:CONSISTS_OF]->(child:Part) "
        f"OPTIONAL MATCH (p)-[:DEFINED_BY]->(d:Drawing) "
        f"OPTIONAL MATCH (p)-[:SUPPLIED_BY]->(s:Supplier) "
        f"RETURN p.part_number, p.name, p.category, p.lifecycle_state, "
        f"count(DISTINCT child), count(DISTINCT d), count(DISTINCT s) "
        f"ORDER BY p.part_number SKIP {offset} LIMIT {limit}"
    )

    return execute_cypher(db, query, graph_name)


def get_part_vertex(db: Session, graph_name: str, part_number: str) -> dict | None:
    """Part vertex 전체 속성 반환"""
    escaped = escape_cypher_value(part_number)
    query = f"MATCH (p:Part {{part_number: '{escaped}'}}) RETURN p"
    rows = execute_cypher(db, query, graph_name)
    if not rows:
        return None
    vertex = rows[0]
    # vertex는 {"id": ..., "label": ..., "properties": {...}} 형태
    if isinstance(vertex, dict) and "properties" in vertex:
        return vertex["properties"]
    return vertex


def get_children(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """CONSISTS_OF 자식 (depth 1)"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[r:CONSISTS_OF]->(c:Part) "
        f"RETURN c.part_number, c.name, r.quantity, r.sequence, "
        f"r.reference_designator, r.find_number "
        f"ORDER BY r.sequence, c.part_number"
    )
    return execute_cypher(db, query, graph_name)


def get_parents(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """CONSISTS_OF 부모 (depth 1)"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (c:Part {{part_number: '{escaped}'}})<-[r:CONSISTS_OF]-(p:Part) "
        f"RETURN p.part_number, p.name, r.quantity, r.sequence, "
        f"r.reference_designator, r.find_number "
        f"ORDER BY p.part_number"
    )
    return execute_cypher(db, query, graph_name)


def get_drawings(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """DEFINED_BY 도면"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[:DEFINED_BY]->(d:Drawing) "
        f"RETURN d.drawing_number, d.name, d.version, d.status "
        f"ORDER BY d.drawing_number"
    )
    return execute_cypher(db, query, graph_name)


def get_suppliers(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """SUPPLIED_BY 공급사"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[r:SUPPLIED_BY]->(s:Supplier) "
        f"RETURN s.company_name, s.code, s.country, r.unit_cost "
        f"ORDER BY s.company_name"
    )
    return execute_cypher(db, query, graph_name)


def get_bom_paths(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """BOM 전체 경로 (가변 길이 CONSISTS_OF)"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH path = (root:Part {{part_number: '{escaped}'}})"
        f"-[:CONSISTS_OF*]->(desc:Part) "
        f"RETURN [n IN nodes(path) | n.part_number], "
        f"[n IN nodes(path) | n.name], "
        f"[r IN relationships(path) | r.quantity], "
        f"[r IN relationships(path) | r.reference_designator]"
    )
    return execute_cypher(db, query, graph_name)
