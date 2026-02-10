"""활성화 및 탐색 도메인 서비스 레이어."""

import json
import time

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.infrastructure.llm_client import chat_completion_with_usage
from app.modules.activation import repository as repo
from app.modules.activation.schemas import (
    HealthCheckIssue,
    HealthCheckResponse,
    QueryResponse,
    StarterQuestion,
    StartersResponse,
)
from app.modules.ai_usage.service import log_ai_usage
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY


def health_check(
    db: Session,
    auth: AuthContext,
) -> HealthCheckResponse:
    t0 = time.perf_counter()
    graph_name = org_id_to_schema(auth.org_id)
    node_counts = repo.count_nodes_by_labels(
        db,
        graph_name,
        MANUFACTURING_ONTOLOGY.get_valid_labels(),
    )
    rel_counts = repo.count_relationships_by_types(
        db,
        graph_name,
        [rt.rel_type for rt in MANUFACTURING_ONTOLOGY.relationship_types],
    )
    logger.info("[헬스체크] 카운트 조회: {elapsed:.2f}s", elapsed=time.perf_counter() - t0)

    total_nodes = sum(node_counts.values())
    total_rels = sum(rel_counts.values())
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

    orphan_count = repo.count_orphan_parts(db, graph_name, total_parts)
    if orphan_count > 0:
        issues.append(
            HealthCheckIssue(
                category="orphan_parts",
                severity="warning",
                message=f"어떤 프로젝트나 조립체에도 소속되지 않은 부품 {orphan_count}개",
                count=orphan_count,
            )
        )

    no_drawing = repo.count_parts_without_drawing(db, graph_name, total_parts)
    if no_drawing > 0:
        issues.append(
            HealthCheckIssue(
                category="missing_drawing",
                severity="info",
                message=f"도면이 연결되지 않은 부품 {no_drawing}개",
                count=no_drawing,
            )
        )

    no_supplier = repo.count_parts_without_supplier(db, graph_name, total_parts)
    if no_supplier > 0:
        issues.append(
            HealthCheckIssue(
                category="missing_supplier",
                severity="info",
                message=f"공급사가 연결되지 않은 부품 {no_supplier}개",
                count=no_supplier,
            )
        )

    incomplete_bom = repo.count_leaf_parts_without_bom(db, graph_name)
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


def query_graph(
    db: Session,
    auth: AuthContext,
    question: str,
) -> QueryResponse:
    t_total = time.perf_counter()
    graph_name = org_id_to_schema(auth.org_id)

    t0 = time.perf_counter()
    node_counts = repo.count_nodes_by_labels(
        db,
        graph_name,
        MANUFACTURING_ONTOLOGY.get_valid_labels(),
    )
    rel_counts = repo.count_relationships_by_types(
        db,
        graph_name,
        [rt.rel_type for rt in MANUFACTURING_ONTOLOGY.relationship_types],
    )
    ext_hints = repo.list_extended_hints(db)
    logger.info("[질의] 컨텍스트 수집: {elapsed:.2f}s", elapsed=time.perf_counter() - t0)

    system_prompt = _build_tenant_query_prompt(ext_hints, node_counts, rel_counts)

    cypher_resp = chat_completion_with_usage(
        system_prompt=system_prompt,
        user_message=question,
    )
    cypher = cypher_resp.content
    _validate_read_only(cypher)

    log_ai_usage(
        org_id=auth.org_id,
        user_id=auth.account_id,
        feature="activation:cypher",
        model=cypher_resp.model,
        input_tokens=cypher_resp.input_tokens,
        output_tokens=cypher_resp.output_tokens,
    )

    try:
        t0 = time.perf_counter()
        raw_results = repo.execute_graph_query(db, cypher, graph_name)
        logger.info("[질의] Cypher 실행: {elapsed:.2f}s ({count}건)", elapsed=time.perf_counter() - t0, count=len(raw_results))
    except Exception as error:
        logger.warning(
            "Cypher 실행 실패: query={cypher} error={err}",
            cypher=cypher,
            err=error,
        )
        raise AppError(
            message=f"쿼리 실행에 실패했습니다: {error}",
            code="QUERY_EXECUTION_FAILED",
        )

    results = _serialize_results(raw_results)

    answer_input = f"""## 사용자 질문
{question}

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

    total_elapsed = time.perf_counter() - t_total
    logger.info(
        "질의 완료: question={q} results={count}건 총 {elapsed:.1f}s",
        q=question[:50],
        count=len(results),
        elapsed=total_elapsed,
    )
    return QueryResponse(cypher_query=cypher, results=results, answer=answer)


def get_starters() -> StartersResponse:
    return StartersResponse(starters=DEFAULT_STARTERS)


def _build_graph_summary(
    node_counts: dict[str, int],
    rel_counts: dict[str, int],
) -> str:
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
    upper = cypher.upper()
    for keyword in ("CREATE", "MERGE", "DELETE", "SET ", "REMOVE", "DROP"):
        if keyword in upper:
            raise AppError(
                message=f"데이터 변경 쿼리는 허용되지 않습니다: {keyword}",
                code="FORBIDDEN",
            )


def _serialize_results(raw_results: list) -> list[dict]:
    results = []
    for row in raw_results:
        if isinstance(row, dict):
            cleaned = {}
            for key, value in row.items():
                if isinstance(value, dict):
                    cleaned[key] = value.get("properties", value)
                else:
                    cleaned[key] = value
            results.append(cleaned)
        else:
            results.append({"result": str(row)})
    return results


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
