"""아이템(Part) 조회 — 관계는 AGE Cypher, 속성은 RDS."""

from sqlalchemy.orm import Session

from app.infrastructure.age_client import execute_cypher
from app.modules.ontology.repository import escape_cypher_value


def get_children(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """CONSISTS_OF 자식 (depth 1) — part_number + 관계 속성만 반환"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (p:Part {{part_number: '{escaped}'}})-[r:CONSISTS_OF]->(c:Part) "
        f"RETURN c.part_number, r.quantity, r.sequence, "
        f"r.reference_designator, r.find_number "
        f"ORDER BY r.sequence, c.part_number"
    )
    return execute_cypher(db, query, graph_name)


def get_parents(db: Session, graph_name: str, part_number: str) -> list[dict]:
    """CONSISTS_OF 부모 (depth 1) — part_number + 관계 속성만 반환"""
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH (c:Part {{part_number: '{escaped}'}})<-[r:CONSISTS_OF]-(p:Part) "
        f"RETURN p.part_number, r.quantity, r.sequence, "
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
    """BOM 전체 경로 (가변 길이 CONSISTS_OF)

    AGE는 list comprehension 내 property 접근(n.prop)을 지원하지 않으므로
    nodes(path)/relationships(path)를 반환 후 Python에서 파싱합니다.
    """
    escaped = escape_cypher_value(part_number)
    query = (
        f"MATCH path = (root:Part {{part_number: '{escaped}'}})"
        f"-[:CONSISTS_OF*]->(child:Part) "
        f"RETURN nodes(path), relationships(path)"
    )
    raw = execute_cypher(db, query, graph_name)
    results = []
    for row in raw:
        nodes = row["c0"] if isinstance(row, dict) else row[0]
        rels = row["c1"] if isinstance(row, dict) else row[1]
        pn_list = []
        for v in (nodes or []):
            props = v.get("properties", {}) if isinstance(v, dict) else {}
            pn_list.append(props.get("part_number"))
        qty_list = []
        ref_list = []
        for e in (rels or []):
            props = e.get("properties", {}) if isinstance(e, dict) else {}
            qty_list.append(props.get("quantity"))
            ref_list.append(props.get("reference_designator"))
        results.append({"c0": pn_list, "c1": qty_list, "c2": ref_list})
    return results
