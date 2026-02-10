"""활성화 및 탐색(Activation) API 라우터.

구축된 지식 그래프의 품질 검증, AI 대화형 질의, 추천 질문을 제공합니다.
schema-per-tenant 기반으로 _org_id 없이 테넌트 격리를 보장합니다.
"""

import json

from fastapi import APIRouter, Depends
from loguru import logger
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.api.deps import get_tenant_db, require_auth
from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.infrastructure.age_client import execute_cypher
from app.infrastructure.llm_client import chat_completion_with_usage
from app.modules.ai_usage.service import log_ai_usage
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY

router = APIRouter(prefix="/api/v1/activation", tags=["activation"])


# === Pydantic 스키마 ===


class HealthCheckIssue(BaseModel):
    """헬스 체크 개별 이슈."""

    category: str  # orphan_parts | missing_drawing | missing_supplier | missing_bom
    severity: str  # warning | info
    message: str
    count: int


class HealthCheckResponse(BaseModel):
    """헬스 체크 리포트 응답."""

    total_nodes: int
    total_relationships: int
    node_counts: dict[str, int]
    relationship_counts: dict[str, int]
    issues: list[HealthCheckIssue]


class QueryRequest(BaseModel):
    """자연어 질의 요청."""

    question: str


class QueryResponse(BaseModel):
    """자연어 질의 응답."""

    cypher_query: str
    results: list[dict]
    answer: str


class StarterQuestion(BaseModel):
    """추천 질문."""

    question: str
    description: str


class StartersResponse(BaseModel):
    """추천 질문 목록 응답."""

    starters: list[StarterQuestion]


# === 헬스 체크 Cypher 쿼리 ===


def _count_nodes_by_label(db: Session, graph_name: str) -> dict[str, int]:
    """라벨별 노드 수 조회."""
    counts: dict[str, int] = {}
    for label in MANUFACTURING_ONTOLOGY.get_valid_labels():
        try:
            rows = execute_cypher(db, f"MATCH (n:{label}) RETURN count(n)", graph_name)
            counts[label] = rows[0] if rows else 0
        except Exception:
            counts[label] = 0
    return counts


def _count_relationships_by_type(db: Session, graph_name: str) -> dict[str, int]:
    """관계 타입별 수 조회."""
    counts: dict[str, int] = {}
    for rt in MANUFACTURING_ONTOLOGY.relationship_types:
        try:
            rows = execute_cypher(
                db,
                f"MATCH ()-[r:{rt.rel_type}]->() RETURN count(r)",
                graph_name,
            )
            counts[rt.rel_type] = rows[0] if rows else 0
        except Exception:
            counts[rt.rel_type] = 0
    return counts


def _find_orphan_parts(db: Session, graph_name: str, total_parts: int) -> int:
    """어떤 프로젝트에도 소속되지 않고, 상위 조립체도 없는 고립 부품 수.

    AGE는 WHERE NOT ()-[:REL]->() 패턴을 지원하지 않으므로,
    전체 - 연결된 부품 수 = 고립 부품 수로 계산합니다.
    """
    connected = 0
    # CONSISTS_OF 대상인 부품 수
    try:
        rows = execute_cypher(
            db,
            "MATCH ()-[:CONSISTS_OF]->(p:Part) RETURN count(DISTINCT p)",
            graph_name,
        )
        connected += rows[0] if rows else 0
    except Exception:
        db.rollback()

    # HAS_ITEM 대상인 부품 수
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


def _find_parts_without_drawing(db: Session, graph_name: str, total_parts: int) -> int:
    """도면이 연결되지 않은 부품 수."""
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


def _find_parts_without_supplier(db: Session, graph_name: str, total_parts: int) -> int:
    """공급사가 연결되지 않은 부품 수."""
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


def _find_leaf_parts_without_bom(db: Session, graph_name: str) -> int:
    """BOM 하위 부품으로만 존재하고 자체 상세정보가 부족한 부품 수 (name 없음)."""
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


# === 자연어 질의 (schema-per-tenant) ===


def _build_graph_summary(
    node_counts: dict[str, int],
    rel_counts: dict[str, int],
) -> str:
    """현재 그래프 데이터 요약 텍스트 생성."""
    node_lines = [
        f"  - {label}: {count}개" for label, count in node_counts.items() if count > 0
    ]
    rel_lines = [
        f"  - {rel}: {count}개" for rel, count in rel_counts.items() if count > 0
    ]

    if not node_lines:
        return "\n## 현재 그래프 상태\n데이터가 없습니다.\n"

    return f"""
## 현재 그래프 상태 (실제 데이터 기준 — 반드시 참고하세요)
노드:
{chr(10).join(node_lines)}
관계:
{chr(10).join(rel_lines) if rel_lines else "  - (관계 없음)"}

**중요**: 위 통계에 0개인 노드 라벨이나 관계 타입은 데이터가 없으므로 쿼리하지 마세요.
존재하는 데이터만 활용하여 쿼리를 생성하세요.
"""


def _build_tenant_query_prompt(
    extended_hints: list[str],
    node_counts: dict[str, int] | None = None,
    rel_counts: dict[str, int] | None = None,
) -> str:
    """schema-per-tenant용 질의 시스템 프롬프트 (_org_id 불필요)."""
    ext_section = ""
    if extended_hints:
        ext_list = "\n".join(f"  - {h}" for h in extended_hints)
        ext_section = f"""
## 확장 속성 (이 테넌트에서 사용 가능)
{ext_list}
확장 속성은 `_ext_` 프리픽스가 붙어 있으며, 일반 속성처럼 WHERE 절에서 사용 가능합니다.
"""

    graph_summary = ""
    if node_counts is not None and rel_counts is not None:
        graph_summary = _build_graph_summary(node_counts, rel_counts)

    return f"""당신은 Apache AGE (PostgreSQL 그래프 DB) Cypher 쿼리 생성 전문가입니다.
사용자의 자연어 질문을 Cypher 쿼리로 변환하세요.

{MANUFACTURING_ONTOLOGY.to_llm_prompt()}
{ext_section}
{graph_summary}

## 쿼리 규칙
1. MATCH 쿼리만 생성하세요. CREATE/MERGE/DELETE/SET은 절대 금지입니다.
2. 반드시 유효한 Cypher 문법을 사용하세요.
3. 결과는 Cypher 쿼리만 출력하세요 (설명 없이).
4. 노드 라벨과 관계 타입은 위에 정의된 것만 사용하세요.
5. 속성명은 정확히 위에 정의된 이름을 사용하세요.
6. 테넌트 격리는 그래프 레벨에서 이미 보장되므로 _org_id 조건을 추가하지 마세요.
7. 질문이 모호하거나 광범위하면, 존재하는 데이터를 기반으로 유용한 개요를 보여주는 쿼리를 생성하세요.
"""


ANSWER_SYSTEM_PROMPT = """당신은 제조업 데이터 분석 전문가입니다.
사용자의 질문과 그래프 DB 쿼리 결과를 바탕으로 간결하고 유용한 답변을 작성하세요.

## 답변 규칙
1. 한국어로 답변하세요.
2. 데이터 결과를 자연스러운 문장으로 해석하세요.
3. 결과가 비어있으면 "해당하는 데이터가 없습니다"라고 안내하세요.
4. 숫자 데이터가 있으면 핵심 통계를 요약하세요.
5. 3-5문장 이내로 간결하게 답변하세요.
"""


def _validate_read_only(cypher: str) -> None:
    """읽기 전용 쿼리인지 검증."""
    upper = cypher.upper()
    for keyword in ("CREATE", "MERGE", "DELETE", "SET ", "REMOVE", "DROP"):
        if keyword in upper:
            raise AppError(
                message=f"데이터 변경 쿼리는 허용되지 않습니다: {keyword}",
                code="FORBIDDEN",
            )


def _serialize_results(raw_results: list) -> list[dict]:
    """AGE 결과를 JSON 직렬화 가능한 형태로 변환."""
    results = []
    for r in raw_results:
        if isinstance(r, dict):
            cleaned = {}
            for k, v in r.items():
                if isinstance(v, dict):
                    # vertex/edge에서 properties만 추출
                    props = v.get("properties", v)
                    cleaned[k] = props
                else:
                    cleaned[k] = v
            results.append(cleaned)
        else:
            if isinstance(r, dict):
                results.append(r.get("properties", r))
            else:
                results.append({"result": str(r)})
    return results


def _get_extended_hints(db: Session) -> list[str]:
    """테넌트의 확장 속성 힌트 조회 (mapping_records에서 추출)."""
    try:
        from app.modules.mapping.models import MappingRecord

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


# === 추천 질문 ===

DEFAULT_STARTERS = [
    StarterQuestion(
        question="전체 부품 목록을 보여줘",
        description="등록된 모든 부품의 품번과 품명을 조회합니다.",
    ),
    StarterQuestion(
        question="BOM 구조를 보여줘. 상위 부품과 하위 부품의 관계를 알고 싶어",
        description="CONSISTS_OF 관계를 통해 BOM 트리 구조를 탐색합니다.",
    ),
    StarterQuestion(
        question="공급사별로 납품하는 부품 목록을 보여줘",
        description="SUPPLIED_BY 관계를 통해 공급사-부품 매핑을 조회합니다.",
    ),
    StarterQuestion(
        question="도면이 연결되지 않은 부품이 있어?",
        description="DEFINED_BY 관계가 없는 부품을 찾아 데이터 품질을 점검합니다.",
    ),
    StarterQuestion(
        question="단가가 가장 높은 상위 5개 부품을 보여줘",
        description="SUPPLIED_BY 관계의 unit_cost 속성으로 고가 품목을 파악합니다.",
    ),
    StarterQuestion(
        question="프로젝트별 부품 수를 알려줘",
        description="HAS_ITEM 관계를 집계하여 프로젝트 규모를 파악합니다.",
    ),
]


# === API 엔드포인트 ===


@router.post("/health-check", response_model=HealthCheckResponse)
def health_check(
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """지식 그래프 품질 진단 리포트."""
    graph_name = org_id_to_schema(auth.org_id)

    # 노드/관계 카운트
    node_counts = _count_nodes_by_label(db, graph_name)
    rel_counts = _count_relationships_by_type(db, graph_name)

    total_nodes = sum(node_counts.values())
    total_rels = sum(rel_counts.values())

    # 이슈 탐지
    issues: list[HealthCheckIssue] = []
    total_parts = node_counts.get("Part", 0)

    if total_nodes == 0:
        issues.append(
            HealthCheckIssue(
                category="empty_graph",
                severity="warning",
                message="지식 그래프에 데이터가 없습니다. 먼저 데이터를 합성해주세요.",
                count=0,
            )
        )
        return HealthCheckResponse(
            total_nodes=0,
            total_relationships=0,
            node_counts=node_counts,
            relationship_counts=rel_counts,
            issues=issues,
        )

    # 고립 부품 탐지
    orphan_count = _find_orphan_parts(db, graph_name, total_parts)
    if orphan_count > 0:
        issues.append(
            HealthCheckIssue(
                category="orphan_parts",
                severity="warning",
                message=f"어떤 프로젝트나 조립체에도 소속되지 않은 부품 {orphan_count}개",
                count=orphan_count,
            )
        )

    # 도면 미연결 부품
    no_drawing = _find_parts_without_drawing(db, graph_name, total_parts)
    if no_drawing > 0:
        issues.append(
            HealthCheckIssue(
                category="missing_drawing",
                severity="info",
                message=f"도면이 연결되지 않은 부품 {no_drawing}개",
                count=no_drawing,
            )
        )

    # 공급사 미연결 부품
    no_supplier = _find_parts_without_supplier(db, graph_name, total_parts)
    if no_supplier > 0:
        issues.append(
            HealthCheckIssue(
                category="missing_supplier",
                severity="info",
                message=f"공급사가 연결되지 않은 부품 {no_supplier}개",
                count=no_supplier,
            )
        )

    # BOM에는 있지만 상세정보 부족
    incomplete_bom = _find_leaf_parts_without_bom(db, graph_name)
    if incomplete_bom > 0:
        issues.append(
            HealthCheckIssue(
                category="incomplete_bom",
                severity="warning",
                message=f"BOM에 존재하지만 품명 정보가 없는 부품 {incomplete_bom}개",
                count=incomplete_bom,
            )
        )

    logger.info(
        "헬스 체크 완료: 노드={nodes} 관계={rels} 이슈={issues}개",
        nodes=total_nodes,
        rels=total_rels,
        issues=len(issues),
    )

    return HealthCheckResponse(
        total_nodes=total_nodes,
        total_relationships=total_rels,
        node_counts=node_counts,
        relationship_counts=rel_counts,
        issues=issues,
    )


@router.post("/query", response_model=QueryResponse)
def query_graph(
    req: QueryRequest,
    auth: AuthContext = Depends(require_auth),
    db: Session = Depends(get_tenant_db),
):
    """자연어 → Cypher → 그래프 질의 + AI 답변 생성."""
    graph_name = org_id_to_schema(auth.org_id)

    # 그래프 상태 + 확장 속성 힌트
    node_counts = _count_nodes_by_label(db, graph_name)
    rel_counts = _count_relationships_by_type(db, graph_name)
    ext_hints = _get_extended_hints(db)
    system_prompt = _build_tenant_query_prompt(ext_hints, node_counts, rel_counts)

    # 1. 자연어 → Cypher
    cypher_resp = chat_completion_with_usage(
        system_prompt=system_prompt, user_message=req.question
    )
    cypher = cypher_resp.content
    _validate_read_only(cypher)

    # AI 사용량 로깅 — Cypher 생성
    log_ai_usage(
        org_id=auth.org_id,
        user_id=auth.account_id,
        feature="activation:cypher",
        model=cypher_resp.model,
        input_tokens=cypher_resp.input_tokens,
        output_tokens=cypher_resp.output_tokens,
    )

    # 2. Cypher 실행
    try:
        raw_results = execute_cypher(db, cypher, graph_name)
    except Exception as e:
        logger.warning(
            "Cypher 실행 실패: query={cypher} error={err}",
            cypher=cypher,
            err=e,
        )
        raise AppError(
            message=f"쿼리 실행에 실패했습니다: {e}",
            code="QUERY_EXECUTION_FAILED",
        )

    results = _serialize_results(raw_results)

    # 3. 결과 해석 (AI 답변 생성)
    answer_input = f"""## 사용자 질문
{req.question}

## 실행된 Cypher 쿼리
{cypher}

## 쿼리 결과
{json.dumps(results[:50], ensure_ascii=False, default=str)}
"""
    try:
        answer_resp = chat_completion_with_usage(
            system_prompt=ANSWER_SYSTEM_PROMPT,
            user_message=answer_input,
        )
        answer = answer_resp.content

        # AI 사용량 로깅 — 답변 생성
        log_ai_usage(
            org_id=auth.org_id,
            user_id=auth.account_id,
            feature="activation:answer",
            model=answer_resp.model,
            input_tokens=answer_resp.input_tokens,
            output_tokens=answer_resp.output_tokens,
        )
    except Exception:
        answer = "쿼리 결과를 확인해주세요."

    logger.info(
        "질의 완료: question={q} results={count}건",
        q=req.question[:50],
        count=len(results),
    )

    return QueryResponse(
        cypher_query=cypher,
        results=results,
        answer=answer,
    )


@router.get("/starters", response_model=StartersResponse)
def get_starters(
    _auth: AuthContext = Depends(require_auth),
):
    """추천 질문 목록 반환."""
    return StartersResponse(starters=DEFAULT_STARTERS)
