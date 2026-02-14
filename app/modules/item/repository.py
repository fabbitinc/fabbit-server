"""아이템(Part) 조회 — 비-BOM 관계는 AGE Cypher로 조회."""

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.ontology.repository import escape_cypher_value


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
