"""샘플 데이터 시드 스크립트.

Apache AGE 그래프에 제조업 온톨로지 샘플 데이터를 생성합니다.
테넌트 격리를 위해 _org_id 속성을 포함합니다.

구조:
  Assembly-001 (Parent)
  ├── Part-001 (Bracket) ─ Drawing-001, Steel, SupplierA
  ├── Part-002 (Shaft)   ─ Drawing-002, Aluminum, SupplierB
  └── Part-003 (Bearing) ─ Drawing-003, Stainless Steel, SupplierC

사용법:
  docker compose up -d
  uv run alembic upgrade head
  uv run python seed_data.py
"""

from app.core.config import settings
from app.core.database import SessionLocal
from app.infrastructure.age_client import execute_cypher, execute_cypher_raw

ORG_ID = "org_demo"  # 데모용 테넌트 ID

# 시드 Cypher 쿼리들 (_org_id 포함)
SEED_QUERIES = [
    # === 재질(Material) 노드 ===
    f"MERGE (:Material {{name: 'Steel', specification: 'SS400', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Material {{name: 'Aluminum', specification: 'A6061-T6', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Material {{name: 'Stainless Steel', specification: 'SUS304', _org_id: '{ORG_ID}'}})",

    # === 공급업체(Supplier) 노드 ===
    f"MERGE (:Supplier {{name: 'SupplierA', code: 'SUP-A', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Supplier {{name: 'SupplierB', code: 'SUP-B', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Supplier {{name: 'SupplierC', code: 'SUP-C', _org_id: '{ORG_ID}'}})",

    # === 도면(Drawing) 노드 ===
    f"MERGE (:Drawing {{drawing_number: 'DWG-001', name: 'Drawing-001', revision: 'A', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Drawing {{drawing_number: 'DWG-002', name: 'Drawing-002', revision: 'B', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Drawing {{drawing_number: 'DWG-003', name: 'Drawing-003', revision: 'A', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Drawing {{drawing_number: 'DWG-ASM-001', name: 'Drawing-ASM', revision: 'C', _org_id: '{ORG_ID}'}})",

    # === 부품(Part) 노드 ===
    f"MERGE (:Part {{part_number: 'ASM-001', name: 'Assembly-001', description: 'Main Assembly', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Part {{part_number: 'PRT-001', name: 'Bracket', description: 'Support Bracket', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Part {{part_number: 'PRT-002', name: 'Shaft', description: 'Drive Shaft', _org_id: '{ORG_ID}'}})",
    f"MERGE (:Part {{part_number: 'PRT-003', name: 'Bearing', description: 'Ball Bearing', _org_id: '{ORG_ID}'}})",

    # === CONSISTS_OF 관계 (Assembly → Child Parts) ===
    f"""
    MATCH (parent:Part {{part_number: 'ASM-001', _org_id: '{ORG_ID}'}}),
          (child:Part {{part_number: 'PRT-001', _org_id: '{ORG_ID}'}})
    MERGE (parent)-[:CONSISTS_OF {{quantity: 2}}]->(child)
    """,
    f"""
    MATCH (parent:Part {{part_number: 'ASM-001', _org_id: '{ORG_ID}'}}),
          (child:Part {{part_number: 'PRT-002', _org_id: '{ORG_ID}'}})
    MERGE (parent)-[:CONSISTS_OF {{quantity: 1}}]->(child)
    """,
    f"""
    MATCH (parent:Part {{part_number: 'ASM-001', _org_id: '{ORG_ID}'}}),
          (child:Part {{part_number: 'PRT-003', _org_id: '{ORG_ID}'}})
    MERGE (parent)-[:CONSISTS_OF {{quantity: 4}}]->(child)
    """,

    # === DEFINED_BY 관계 (Part → Drawing) ===
    f"""
    MATCH (p:Part {{part_number: 'ASM-001', _org_id: '{ORG_ID}'}}),
          (d:Drawing {{drawing_number: 'DWG-ASM-001', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:DEFINED_BY]->(d)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-001', _org_id: '{ORG_ID}'}}),
          (d:Drawing {{drawing_number: 'DWG-001', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:DEFINED_BY]->(d)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-002', _org_id: '{ORG_ID}'}}),
          (d:Drawing {{drawing_number: 'DWG-002', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:DEFINED_BY]->(d)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-003', _org_id: '{ORG_ID}'}}),
          (d:Drawing {{drawing_number: 'DWG-003', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:DEFINED_BY]->(d)
    """,

    # === MADE_OF 관계 (Part → Material) ===
    f"""
    MATCH (p:Part {{part_number: 'PRT-001', _org_id: '{ORG_ID}'}}),
          (m:Material {{name: 'Steel', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:MADE_OF]->(m)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-002', _org_id: '{ORG_ID}'}}),
          (m:Material {{name: 'Aluminum', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:MADE_OF]->(m)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-003', _org_id: '{ORG_ID}'}}),
          (m:Material {{name: 'Stainless Steel', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:MADE_OF]->(m)
    """,

    # === SUPPLIED_BY 관계 (Part → Supplier) ===
    f"""
    MATCH (p:Part {{part_number: 'PRT-001', _org_id: '{ORG_ID}'}}),
          (s:Supplier {{name: 'SupplierA', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:SUPPLIED_BY]->(s)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-002', _org_id: '{ORG_ID}'}}),
          (s:Supplier {{name: 'SupplierB', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:SUPPLIED_BY]->(s)
    """,
    f"""
    MATCH (p:Part {{part_number: 'PRT-003', _org_id: '{ORG_ID}'}}),
          (s:Supplier {{name: 'SupplierC', _org_id: '{ORG_ID}'}})
    MERGE (p)-[:SUPPLIED_BY]->(s)
    """,
]


def seed():
    db = SessionLocal()
    graph = settings.graph_name

    print(f"그래프 '{graph}'에 시드 데이터 삽입 시작... (org_id: {ORG_ID})")

    for i, query in enumerate(SEED_QUERIES, 1):
        query = query.strip()
        try:
            execute_cypher_raw(db, query)
            db.commit()
            print(f"  [{i}/{len(SEED_QUERIES)}] 완료")
        except Exception as e:
            db.rollback()
            print(f"  [{i}/{len(SEED_QUERIES)}] 실패: {e}")

    # 결과 확인
    print("\n=== 생성된 노드 ===")
    nodes = execute_cypher(db, "MATCH (n) RETURN n")
    db.commit()
    for node in nodes:
        print(f"  {node}")

    print("\n=== 생성된 관계 ===")
    rels = execute_cypher(db, "MATCH (a)-[r]->(b) RETURN a, r, b")
    db.commit()
    for rel in rels:
        print(f"  {rel['c0']} --[{rel['c1']}]--> {rel['c2']}")

    db.close()
    print("\n시드 데이터 삽입 완료!")


if __name__ == "__main__":
    seed()
