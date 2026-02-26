"""활성화 및 탐색 도메인 서비스 레이어.

LLM이 JSON 쿼리 플랜을 생성하고, mode에 따라 SQL/Graph/Hybrid로 실행합니다.
- sql: Part 속성만 필요 → RDS parts 테이블 조회
- graph: 관계 탐색 필요 → Cypher 실행 후 RDS enrichment
- hybrid: SQL pre-filter → Cypher 관계 탐색 → RDS enrichment
"""

import json
import re
import time

from loguru import logger
from sqlalchemy.orm import Session

from app.core.auth_context import AuthContext
from app.core.exceptions import AppError
from app.infrastructure.ai_usage_logger import log_ai_usage
from app.infrastructure.llm_client import chat_completion_with_usage
from app.modules.activation import repository as repo
from app.modules.activation.schemas import (
    HealthCheckIssue,
    HealthCheckResponse,
    QueryResponse,
    StartersResponse,
)
from app.modules.auth.provisioning import org_id_to_schema
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY


def health_check(
    db: Session,
    auth: AuthContext,
) -> HealthCheckResponse:
    graph_name = org_id_to_schema(auth.org_id)
    node_counts = _count_nodes_by_labels(
        db,
        graph_name,
        MANUFACTURING_ONTOLOGY.get_valid_labels(),
    )
    # Part 카운트는 RDS에서 오버라이드
    part_count = repo.count_all_parts(db)
    node_counts["Part"] = part_count

    rel_counts = _count_relationships_by_types(
        db,
        graph_name,
        [rt.rel_type for rt in MANUFACTURING_ONTOLOGY.relationship_types],
    )

    total_nodes = sum(node_counts.values())
    total_rels = sum(rel_counts.values())
    issues: list[HealthCheckIssue] = []
    total_parts = part_count

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

    # 1. 컨텍스트 수집
    t0 = time.perf_counter()
    node_counts = _count_nodes_by_labels(
        db,
        graph_name,
        MANUFACTURING_ONTOLOGY.get_valid_labels(),
    )
    # Part 카운트 RDS 오버라이드
    part_count = repo.count_all_parts(db)
    node_counts["Part"] = part_count

    rel_counts = _count_relationships_by_types(
        db,
        graph_name,
        [rt.rel_type for rt in MANUFACTURING_ONTOLOGY.relationship_types],
    )
    ext_hints = _build_extended_hints(db)
    logger.info(
        "[질의] 컨텍스트 수집: {elapsed:.2f}s", elapsed=time.perf_counter() - t0
    )

    system_prompt = _build_tenant_query_prompt(ext_hints, node_counts, rel_counts)

    # 2. LLM 호출 → JSON 쿼리 플랜
    plan_resp = chat_completion_with_usage(
        system_prompt=system_prompt,
        user_message=question,
        max_tokens=800,
    )
    raw_plan = plan_resp.content
    _log_cypher(stage="initial", query=raw_plan)

    log_ai_usage(
        org_id=auth.org_id,
        user_id=auth.account_id,
        feature="activation:query_plan",
        model=plan_resp.model,
        input_tokens=plan_resp.input_tokens,
        output_tokens=plan_resp.output_tokens,
    )

    query_plan = _parse_query_plan(raw_plan)

    # 3. 모드별 실행
    results = _execute_plan(db, graph_name, query_plan)

    # 4. 결과 0건 시 재시도
    if not results:
        retry_resp = chat_completion_with_usage(
            system_prompt=system_prompt,
            user_message=_build_zero_result_retry_prompt(question, raw_plan),
            max_tokens=800,
        )
        retry_raw = retry_resp.content

        log_ai_usage(
            org_id=auth.org_id,
            user_id=auth.account_id,
            feature="activation:query_plan_retry",
            model=retry_resp.model,
            input_tokens=retry_resp.input_tokens,
            output_tokens=retry_resp.output_tokens,
        )

        if _normalize_query(raw_plan) != _normalize_query(retry_raw):
            _log_cypher(stage="retry", query=retry_raw)
            retry_plan = _parse_query_plan(retry_raw)
            retry_results = _execute_plan(db, graph_name, retry_plan)
            if retry_results:
                raw_plan = retry_raw
                query_plan = retry_plan
                results = retry_results
            else:
                logger.info("[질의] 재시도 쿼리도 0건입니다")
        else:
            logger.info("[질의] 재시도 쿼리가 초기 쿼리와 동일하여 실행을 생략합니다")

    # 5. 답변 생성
    results_for_answer = results[:20]
    results_json = json.dumps(results_for_answer, ensure_ascii=False, default=str)
    if len(results_json) > 3000:
        results_json = results_json[:3000] + "...(truncated)"
    total_note = (
        f"\n\n(총 {len(results)}건 중 상위 {len(results_for_answer)}건 표시)"
        if len(results) > 20
        else ""
    )

    # 쿼리 플랜 요약
    plan_summary = json.dumps(query_plan, ensure_ascii=False)

    answer_input = f"""## 사용자 질문
{question}

## 실행된 쿼리 플랜
{plan_summary}

## 쿼리 결과
{results_json}{total_note}
"""
    try:
        answer_resp = chat_completion_with_usage(
            system_prompt=ANSWER_SYSTEM_PROMPT,
            user_message=answer_input,
            max_tokens=500,
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
        "질의 완료: question={q} mode={mode} results={count}건 총 {elapsed:.1f}s",
        q=question[:50],
        mode=query_plan.get("mode", "unknown"),
        count=len(results),
        elapsed=total_elapsed,
    )
    return QueryResponse(results=results, answer=answer)


def get_starters() -> StartersResponse:
    from app.modules.activation.constants import DEFAULT_STARTERS

    return StartersResponse(starters=DEFAULT_STARTERS)


# ── 쿼리 플랜 파싱 & 실행 ──


def _parse_query_plan(raw: str) -> dict:
    """LLM 응답에서 JSON 쿼리 플랜을 파싱.

    LLM이 ```json ... ``` 블록이나 순수 JSON을 반환할 수 있으므로 양쪽 모두 처리.
    파싱 실패 시 기존 Cypher 호환 모드(graph)로 폴백.
    """
    # ```json ... ``` 블록 추출
    block_match = re.search(r"```(?:json)?\s*\n?(.*?)\n?```", raw, re.DOTALL)
    json_str = block_match.group(1).strip() if block_match else raw.strip()

    try:
        plan = json.loads(json_str)
        if isinstance(plan, dict) and "mode" in plan:
            return plan
    except (json.JSONDecodeError, ValueError):
        pass

    # 폴백: 순수 Cypher로 간주
    logger.warning("[질의] JSON 파싱 실패, Cypher 폴백: {raw}", raw=raw[:200])
    return {"mode": "graph", "cypher": raw.strip(), "part_sql_where": None}


def _execute_plan(db: Session, graph_name: str, plan: dict) -> list[dict]:
    """쿼리 플랜을 모드별로 실행하고 결과를 반환."""
    mode = plan.get("mode", "graph")

    if mode == "sql":
        where = plan.get("part_sql_where", "")
        if not where:
            return []
        return _execute_sql_query(db, where)

    elif mode == "graph":
        cypher = plan.get("cypher", "")
        if not cypher:
            return []
        _validate_read_only(cypher)
        raw = _execute_cypher_with_logging(db, graph_name, cypher, stage="plan")
        return _enrich_cypher_results(db, raw)

    elif mode == "hybrid":
        where = plan.get("part_sql_where", "")
        cypher = plan.get("cypher", "")
        if not where and not cypher:
            return []

        # SQL pre-filter로 후보 part_numbers 확보
        if where:
            candidates = _execute_sql_query(db, where)
            candidate_pns = [
                r["part_number"] for r in candidates if r.get("part_number")
            ]
            if not candidate_pns:
                return []
        else:
            candidate_pns = None

        if cypher:
            _validate_read_only(cypher)
            if candidate_pns is not None:
                cypher = _inject_part_filter(cypher, candidate_pns)
            raw = _execute_cypher_with_logging(db, graph_name, cypher, stage="plan")
            return _enrich_cypher_results(db, raw)
        else:
            # Cypher 없이 SQL만 → sql 모드와 동일
            return candidates if where else []

    else:
        logger.warning("[질의] 알 수 없는 모드: {mode}", mode=mode)
        return []


# ── SQL 쿼리 실행 ──

# 금지 키워드 (DML/DDL)
_FORBIDDEN_SQL_KEYWORDS = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|EXECUTE|EXEC)\b",
    re.IGNORECASE,
)


def _validate_sql_where(where_clause: str) -> None:
    """LLM이 생성한 SQL WHERE 절의 안전성 검증."""
    if _FORBIDDEN_SQL_KEYWORDS.search(where_clause):
        raise AppError(
            message="데이터 변경 SQL은 허용되지 않습니다",
            code="FORBIDDEN",
        )
    # 세미콜론 방지
    if ";" in where_clause:
        raise AppError(
            message="SQL에 세미콜론이 포함되어 있습니다",
            code="FORBIDDEN",
        )


def _execute_sql_query(db: Session, where_clause: str) -> list[dict]:
    """parts 테이블에서 WHERE 조건으로 조회."""
    _validate_sql_where(where_clause)

    try:
        return repo.execute_parts_sql_where(db, where_clause)
    except Exception as e:
        logger.warning("[질의] SQL 실행 실패: where={w} error={e}", w=where_clause, e=e)
        raise AppError(
            message="SQL 쿼리 실행에 실패했습니다",
            code="QUERY_EXECUTION_FAILED",
        )


# ── Cypher 실행 & enrichment ──


def _execute_cypher_with_logging(
    db: Session,
    graph_name: str,
    cypher: str,
    *,
    stage: str,
) -> list:
    try:
        t0 = time.perf_counter()
        raw_results = repo.execute_graph_query(db, cypher, graph_name)
        logger.info(
            "[질의] Cypher 실행({stage}): {elapsed:.2f}s ({count}건)",
            stage=stage,
            elapsed=time.perf_counter() - t0,
            count=len(raw_results),
        )
        return raw_results
    except Exception as error:
        logger.warning(
            "Cypher 실행 실패({stage}): query={cypher} error={err}",
            stage=stage,
            cypher=_compact_query_for_log(cypher),
            err=error,
        )
        raise AppError(
            message="쿼리 실행에 실패했습니다",
            code="QUERY_EXECUTION_FAILED",
        )


def _enrich_cypher_results(db: Session, raw_results: list) -> list[dict]:
    """Cypher 결과를 직렬화하고, part_number가 포함된 경우 RDS에서 속성을 enrichment."""
    serialized = _serialize_results(raw_results)

    # 결과에서 part_number 추출
    part_numbers: set[str] = set()
    for row in serialized:
        pn = row.get("part_number") or row.get("c0")
        if isinstance(pn, str):
            part_numbers.add(pn)

    if not part_numbers:
        return serialized

    # RDS에서 Part 속성 일괄 조회
    parts = repo.get_parts_by_part_numbers(db, list(part_numbers))
    part_map = {p.part_number: p for p in parts}

    # enrichment: part_number가 있는 행에 RDS 속성 병합
    enriched = []
    for row in serialized:
        pn = row.get("part_number") or row.get("c0")
        if isinstance(pn, str) and pn in part_map:
            p = part_map[pn]
            enriched_row = {
                "part_number": p.part_number,
                "name": p.name,
                "category": p.category,
                "material": p.material,
                "lifecycle_state": p.lifecycle_state,
                "revision": p.revision,
                "unit": p.unit,
            }
            # 기존 Cypher 결과의 다른 필드 유지 (관계 속성 등)
            for k, v in row.items():
                if k not in enriched_row:
                    enriched_row[k] = v
            # 확장 속성 추가
            for k, v in (p.extended_properties or {}).items():
                if v is not None:
                    enriched_row[k] = v
            enriched.append(enriched_row)
        else:
            enriched.append(row)
    return enriched


def _inject_part_filter(cypher: str, part_numbers: list[str]) -> str:
    """Cypher에 Part 후보 필터를 주입.

    MATCH (p:Part) 뒤에 WHERE p.part_number IN [...] 조건을 추가합니다.
    """
    if not part_numbers:
        return cypher

    escaped_pns = [f"'{pn.replace(chr(39), chr(92) + chr(39))}'" for pn in part_numbers]
    in_clause = f"[{', '.join(escaped_pns)}]"

    # 기존 WHERE가 있으면 AND로 연결, 없으면 WHERE 추가
    # 단순 패턴 매칭: MATCH 뒤의 첫 WHERE 또는 RETURN 앞에 삽입
    filter_condition = f"p.part_number IN {in_clause}"

    if re.search(r"\bWHERE\b", cypher, re.IGNORECASE):
        # 기존 WHERE 뒤에 AND로 추가
        cypher = re.sub(
            r"\bWHERE\b",
            f"WHERE {filter_condition} AND",
            cypher,
            count=1,
            flags=re.IGNORECASE,
        )
    else:
        # RETURN 앞에 WHERE 삽입
        cypher = re.sub(
            r"\bRETURN\b",
            f"WHERE {filter_condition} RETURN",
            cypher,
            count=1,
            flags=re.IGNORECASE,
        )
    return cypher


# ── 프롬프트 ──


def _count_nodes_by_labels(
    db: Session, graph_name: str, labels: list[str]
) -> dict[str, int]:
    """각 라벨별 노드 수 조회."""
    return {label: repo.count_nodes_by_label(db, graph_name, label) for label in labels}


def _count_relationships_by_types(
    db: Session, graph_name: str, rel_types: list[str]
) -> dict[str, int]:
    """각 관계 타입별 수 조회."""
    return {
        rel_type: repo.count_relationships_by_type(db, graph_name, rel_type)
        for rel_type in rel_types
    }


def _build_extended_hints(db: Session) -> list[str]:
    """최근 매핑에서 확장 속성 힌트를 추출."""
    import json

    try:
        raw_mappings = repo.list_recent_mappings(db)
        ext_props: set[str] = set()
        for mapping_data in raw_mappings:
            if isinstance(mapping_data, str):
                mapping_data = json.loads(mapping_data)
            for pm in mapping_data.get("property_mappings", []):
                prop_name = pm.get("target_property", "")
                source = pm.get("source_column", "")
                if prop_name.startswith("_ext_"):
                    ext_props.add(f"Part.{prop_name} (원본: {source})")
        return sorted(ext_props)
    except Exception:
        return []


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
## 현재 데이터 상태 (실제 데이터 기준 — 반드시 참고하세요)
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
확장 속성은 `_ext_` 프리픽스가 붙어 있습니다. SQL에서는 extended_properties->>'속성명'으로, Cypher에서는 n._ext_xxx로 접근합니다.
"""

    graph_summary = ""
    if node_counts is not None and rel_counts is not None:
        graph_summary = _build_graph_summary(node_counts, rel_counts)

    return f"""당신은 제조업 데이터 조회 전문가입니다.
사용자의 자연어 질문을 분석하여 최적의 쿼리 플랜을 JSON으로 출력하세요.

{MANUFACTURING_ONTOLOGY.to_llm_prompt()}
{ext_section}
{graph_summary}

## 데이터 저장 구조 (중요!)
- **Part 속성**(part_number, name, category, material, revision, unit, description, lifecycle_state, is_phantom, lead_time_days)은 **RDS `parts` 테이블**에 저장
- **Graph Part 노드**에는 `part_number`만 있음 (속성 없음)
- **Supplier, Drawing, Project** 노드는 모든 속성이 **Graph**에 있음
- **관계**(CONSISTS_OF, DEFINED_BY, SUPPLIED_BY, HAS_ITEM)는 **Graph**에 있음

## 출력 형식
반드시 아래 JSON 형식으로 출력하세요. 설명 텍스트 없이 JSON만 출력하세요.

```json
{{
  "mode": "sql" | "graph" | "hybrid",
  "cypher": "MATCH ... RETURN p.part_number, ...",
  "part_sql_where": "category = '구매품' AND name ILIKE '%브라켓%'"
}}
```

## 모드 선택 기준
1. **sql**: Part 속성만으로 답변 가능 (관계 탐색 불필요)
   - 예: "구매품 목록", "SUS304 재질 부품", "리드타임 30일 이상 부품"
   - `part_sql_where`에 PostgreSQL WHERE 조건 작성
   - `cypher`는 빈 문자열

2. **graph**: 관계 탐색이 필요 (Supplier/Drawing/Project 연결 조회)
   - 예: "미스미에서 공급받는 부품", "BOM 구조", "도면 연결 현황"
   - `cypher`에 Apache AGE Cypher 쿼리 작성. RETURN에 p.part_number 필수
   - `part_sql_where`는 빈 문자열

3. **hybrid**: 관계 + Part 속성 필터 둘 다 필요
   - 예: "구매품 중 미스미에서 공급받는 부품"
   - `part_sql_where`로 Part 속성 필터 (SQL pre-filter)
   - `cypher`로 관계 탐색 (시스템이 자동으로 후보 Part 필터를 Cypher에 주입)

## 쿼리 규칙
1. MATCH 쿼리만 생성. CREATE/MERGE/DELETE/SET 절대 금지
2. Cypher에서 Part 노드 속성은 part_number만 접근 가능 (p.name, p.category 등 사용 금지)
3. SQL WHERE에서 문자열 비교는 ILIKE 사용 (대소문자 무시)
4. 관계 방향은 온톨로지 정의 준수: (Part)-[:SUPPLIED_BY]->(Supplier)
5. 테넌트 격리는 자동 보장, _org_id 조건 불필요
6. Graph의 Cypher에서 RETURN에는 반드시 p.part_number를 포함하세요
"""


ANSWER_SYSTEM_PROMPT = """제조업 데이터 분석 전문가. 쿼리 결과를 간결히 요약.

규칙:
- 한국어, 2-3문장 이내
- 결과가 많으면 핵심 통계만 (목록 나열 금지)
- 결과 비어있으면 "해당 데이터가 없습니다"
- 마크다운 사용 금지, 순수 텍스트만
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


def _build_zero_result_retry_prompt(question: str, previous_plan: str) -> str:
    return f"""다음 질의는 이전 쿼리 플랜 실행 결과가 0건이었습니다.
질문의 의도는 유지하면서, 결과가 나오도록 쿼리 플랜을 1회 재작성하세요.

규칙:
- 동일한 JSON 형식으로 출력
- Part 속성 필터는 ILIKE/CONTAINS 등 유연한 매칭 사용
- 문자열 필터는 가능한 경우 ILIKE와 부분 매칭 사용

## 사용자 질문
{question}

## 이전 쿼리 플랜 (0건)
{previous_plan}
"""


def _log_cypher(*, stage: str, query: str) -> None:
    logger.info(
        "[질의] 생성 쿼리({stage}): {cypher}",
        stage=stage,
        cypher=_compact_query_for_log(query),
    )


def _normalize_query(raw: str) -> str:
    return " ".join(raw.split()).strip().lower()


def _compact_query_for_log(raw: str, max_len: int = 2000) -> str:
    compact = " ".join(raw.split())
    if len(compact) <= max_len:
        return compact
    extra = len(compact) - max_len
    return compact[:max_len] + f"...(truncated {extra} chars)"
