"""테넌트 프로비저닝 — AGE 그래프 + 내부 테이블 + 온톨로지 인덱스."""

import uuid

from alembic.config import Config as AlembicConfig
from alembic.script import ScriptDirectory
from loguru import logger
from sqlalchemy.orm import Session

from app.core.database import TenantBase, discover_models

discover_models()
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY


def org_id_to_schema(org_id: uuid.UUID) -> str:
    """UUID → 테넌트 스키마/그래프명 변환 (하이픈 제거)."""
    return f"tenant_{str(org_id).replace('-', '')}"


def provision_tenant(db: Session, org_id: uuid.UUID) -> str:
    """신규 조직의 테넌트 환경을 프로비저닝.

    1. AGE 그래프 생성 (내부적으로 스키마 자동 생성)
    2. 테넌트 내부 테이블 생성 (ORM 모델 기반)
    3. 온톨로지 vlabel + 속성 인덱스 생성

    PostgreSQL DDL은 트랜잭션 내에서 롤백 가능하므로,
    호출자의 트랜잭션에 포함시켜 실패 시 전체 롤백을 보장합니다.
    """
    graph_name = org_id_to_schema(org_id)
    conn = db.connection()

    # ── 1. AGE 그래프 생성 ──
    result = conn.exec_driver_sql(
        "SELECT count(*) FROM ag_catalog.ag_graph WHERE name = %s", (graph_name,)
    )
    if result.scalar() == 0:
        conn.exec_driver_sql(f"SELECT create_graph('{graph_name}')")
        logger.info("AGE 그래프 생성: {graph}", graph=graph_name)

    # ── 2. 테넌트 내부 테이블 생성 (ORM 모델 기반) ──
    conn.exec_driver_sql(f"SET search_path = {graph_name}, ag_catalog, public")
    TenantBase.metadata.create_all(bind=conn)

    # ── 2-1. 현재 tenant 마이그레이션 head를 스탬프 ──
    _stamp_tenant_alembic_version(conn, graph_name)

    # search_path 복원 (이후 작업이 ag_catalog를 참조하므로)
    conn.exec_driver_sql("SET search_path = ag_catalog, public")

    # ── 3. 온톨로지 vlabel + 인덱스 생성 ──
    _create_ontology_indexes(conn, graph_name)

    return graph_name


def _stamp_tenant_alembic_version(conn, schema_name: str) -> None:
    """테넌트 스키마에 현재 tenant 마이그레이션 head revision을 기록.

    create_all()로 테이블을 생성한 직후 호출하여,
    이후 upgrade head 실행 시 이미 적용된 것으로 인식시킵니다.
    """
    alembic_cfg = AlembicConfig("alembic_tenant.ini")
    script = ScriptDirectory.from_config(alembic_cfg)
    head = script.get_current_head()

    if head is None:
        # 마이그레이션 파일이 아직 없으면 skip
        return

    # search_path가 이미 tenant 스키마로 설정된 상태
    conn.exec_driver_sql(
        f"CREATE TABLE IF NOT EXISTS {schema_name}.alembic_version ("
        "version_num VARCHAR(32) NOT NULL, "
        "CONSTRAINT alembic_version_pkc PRIMARY KEY (version_num))"
    )
    conn.exec_driver_sql(
        f"INSERT INTO {schema_name}.alembic_version (version_num) "
        f"VALUES (%s) ON CONFLICT DO NOTHING",
        (head,),
    )
    logger.info(
        "테넌트 alembic_version 스탬프: {schema} → {rev}",
        schema=schema_name,
        rev=head,
    )


def _create_ontology_indexes(conn, graph_name: str) -> None:
    """온톨로지 정의 기반 vlabel 생성 + merge key 인덱스."""
    indexed = MANUFACTURING_ONTOLOGY.get_all_indexed_properties()

    # vlabel 먼저 생성 (인덱스 대상 라벨만)
    created_labels: set[str] = set()
    for label, _ in indexed:
        if label not in created_labels:
            conn.exec_driver_sql(f"SELECT create_vlabel('{graph_name}', '{label}')")
            created_labels.add(label)

    # 속성별 B-tree 인덱스 (AGE agtype 컬럼 → agtype_access_operator 사용)
    for label, prop_name in indexed:
        index_name = f"ix_{label.lower()}_{prop_name}"
        conn.exec_driver_sql(f"""
            CREATE INDEX IF NOT EXISTS {index_name}
            ON {graph_name}."{label}"
            USING btree ((ag_catalog.agtype_access_operator(properties, '"{prop_name}"'::agtype)))
        """)
