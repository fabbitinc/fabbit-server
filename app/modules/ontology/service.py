"""온톨로지 비즈니스 로직.

"무엇을 할 것인가"를 담당합니다:
- LLM 매핑 생성 + 검증
- 배치 인제스션 오케스트레이션
- 자연어 질의 (테넌트 격리)
"""

import json

import pandas as pd
from sqlalchemy.orm import Session

from app.infrastructure.llm_client import chat_completion
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import (
    ColumnMapping,
    ExtendedPropertyMapping,
    IngestionStats,
    MappingResult,
    QueryResponse,
    RelationMapping,
)
from app.modules.ontology import repository as repo

CHUNK_SIZE = 500


# === 매핑 생성 ===

MAPPING_SYSTEM_PROMPT = f"""당신은 제조업 데이터 매핑 전문가입니다.
Excel 스프레드시트의 컬럼 헤더와 샘플 데이터를 분석하여,
아래 온톨로지 스키마에 매핑하세요.

{MANUFACTURING_ONTOLOGY.to_mapping_prompt_text()}

## 매핑 규칙
1. **MERGE KEY가 있는 속성은 반드시 매핑해야 합니다** (part_number, name 등).
2. 온톨로지에 정의된 속성에 해당하는 컬럼은 column_mappings로 분류합니다.
3. 두 노드 간 관계를 유추할 수 있으면 relation_mappings로 분류합니다.
4. 온톨로지에 없는 추가 컬럼은 extended_properties로 분류합니다:
   - property_name은 반드시 `_ext_` 프리픽스를 붙이고, 영문 snake_case로 작성합니다.
   - 예: "탄소배출량" → "_ext_carbon_emission"
5. 매핑할 수 없는 컬럼(빈 값, 의미 없는 인덱스 등)은 무시합니다.

## 데이터 타입 규칙
- 각 매핑에 data_type을 지정하세요: "string", "integer", "float", "boolean"
- 샘플 데이터를 보고 적절한 타입을 추론하세요.
- 수량(quantity), 개수, 가격 등 숫자 → "integer" 또는 "float"
- 이름, 코드, 설명 등 텍스트 → "string"
- 확신이 없으면 "string"으로 지정하세요.

## 출력 형식 (JSON)
```json
{{
  "column_mappings": [
    {{"source_column": "품번", "target_label": "Part", "target_property": "part_number", "data_type": "string"}}
  ],
  "relation_mappings": [
    {{"from_label": "Part", "to_label": "Material", "rel_type": "MADE_OF", "properties": {{}}, "property_types": {{}}}}
  ],
  "extended_properties": [
    {{"source_column": "탄소배출량", "target_label": "Part", "property_name": "_ext_carbon_emission", "data_type": "float"}}
  ]
}}
```

반드시 위 JSON 형식만 출력하세요. 설명이나 마크다운 없이 순수 JSON만 출력하세요.
"""


def generate_mapping(headers: list[str], sample_rows: list[dict]) -> MappingResult:
    """Excel 헤더 + 샘플 데이터로 매핑 생성 (LLM 1회 호출 + 검증)"""
    user_message = f"""다음 Excel 데이터를 분석하여 매핑하세요.

## 컬럼 헤더
{json.dumps(headers, ensure_ascii=False)}

## 샘플 데이터 (처음 5행)
{json.dumps(sample_rows, ensure_ascii=False, indent=2)}
"""

    raw_text = chat_completion(
        system_prompt=MAPPING_SYSTEM_PROMPT,
        user_message=user_message,
        response_format={"type": "json_object"},
    )
    raw = json.loads(raw_text)

    result = MappingResult(
        column_mappings=[ColumnMapping(**cm) for cm in raw.get("column_mappings", [])],
        relation_mappings=[RelationMapping(**rm) for rm in raw.get("relation_mappings", [])],
        extended_properties=[ExtendedPropertyMapping(**ep) for ep in raw.get("extended_properties", [])],
    )

    return _validate_and_fix_mapping(result)


def _validate_and_fix_mapping(result: MappingResult) -> MappingResult:
    """온톨로지 라벨/속성 유효성 검증 + 자동 보정"""
    valid_labels = MANUFACTURING_ONTOLOGY.get_valid_labels()
    valid_rel_types = MANUFACTURING_ONTOLOGY.get_valid_rel_types()

    verified_columns = []
    for cm in result.column_mappings:
        if cm.target_label not in valid_labels:
            result.extended_properties.append(ExtendedPropertyMapping(
                source_column=cm.source_column,
                target_label="Part",
                property_name=f"_ext_{cm.target_property}",
            ))
            continue

        node = MANUFACTURING_ONTOLOGY.get_node_label(cm.target_label)
        valid_props = [p.name for p in node.properties]
        if cm.target_property not in valid_props:
            result.extended_properties.append(ExtendedPropertyMapping(
                source_column=cm.source_column,
                target_label=cm.target_label,
                property_name=f"_ext_{cm.target_property}",
            ))
            continue

        verified_columns.append(cm)

    result.column_mappings = verified_columns

    verified_rels = [
        rm for rm in result.relation_mappings
        if rm.rel_type in valid_rel_types
        and rm.from_label in valid_labels
        and rm.to_label in valid_labels
    ]
    result.relation_mappings = verified_rels

    fixed_ext = []
    for ep in result.extended_properties:
        label = ep.target_label if ep.target_label in valid_labels else "Part"
        name = ep.property_name if ep.property_name.startswith("_ext_") else f"_ext_{ep.property_name}"
        fixed_ext.append(ExtendedPropertyMapping(
            source_column=ep.source_column,
            target_label=label,
            property_name=name,
            data_type=ep.data_type,
        ))
    result.extended_properties = fixed_ext

    return result


# === 배치 인제스션 ===

def ingest_dataframe(
    db: Session,
    df: pd.DataFrame,
    mapping: MappingResult,
    org_id: str,
) -> IngestionStats:
    """DataFrame을 매핑에 따라 그래프 DB에 적재

    1. 500행 청크로 분할
    2. 노드 먼저 MERGE → 관계 MERGE (순서 보장)
    3. 청크 단위 커밋
    """
    stats = IngestionStats(total_rows=len(df), nodes_created=0, relationships_created=0)

    # 라벨별 매핑 그룹핑
    label_mappings: dict[str, list] = {}
    for cm in mapping.column_mappings:
        label_mappings.setdefault(cm.target_label, []).append(cm)

    ext_mappings: dict[str, list] = {}
    for ep in mapping.extended_properties:
        ext_mappings.setdefault(ep.target_label, []).append(ep)

    rel_prop_types: dict[str, dict[str, str]] = {}
    for rm in mapping.relation_mappings:
        rel_prop_types[rm.rel_type] = rm.property_types

    for chunk_start in range(0, len(df), CHUNK_SIZE):
        chunk = df.iloc[chunk_start:chunk_start + CHUNK_SIZE]

        # Phase 1: 노드 MERGE
        for _, row in chunk.iterrows():
            for label, col_maps in label_mappings.items():
                node_def = MANUFACTURING_ONTOLOGY.get_node_label(label)
                if not node_def:
                    continue

                merge_keys = {}
                set_props = {}
                for cm in col_maps:
                    val = row.get(cm.source_column)
                    if cm.target_property in node_def.merge_keys:
                        formatted = repo.format_cypher_value(val, "string")
                        if formatted is None:
                            continue
                        merge_keys[cm.target_property] = formatted
                    else:
                        formatted = repo.format_cypher_value(val, cm.data_type)
                        if formatted is None:
                            continue
                        set_props[cm.target_property] = formatted

                for ep in ext_mappings.get(label, []):
                    val = row.get(ep.source_column)
                    formatted = repo.format_cypher_value(val, ep.data_type)
                    if formatted is None:
                        continue
                    set_props[ep.property_name] = formatted

                if not merge_keys:
                    continue

                cypher = repo.build_merge_node_cypher(label, merge_keys, set_props, org_id)
                try:
                    repo.execute_graph_merge(db, cypher)
                    stats.nodes_created += 1
                except Exception as e:
                    db.rollback()
                    stats.errors.append(f"노드 MERGE 실패 [{label}]: {e}")

        # Phase 2: 관계 MERGE
        for _, row in chunk.iterrows():
            for rm in mapping.relation_mappings:
                from_node = MANUFACTURING_ONTOLOGY.get_node_label(rm.from_label)
                to_node = MANUFACTURING_ONTOLOGY.get_node_label(rm.to_label)
                if not from_node or not to_node:
                    continue

                from_keys = {}
                for cm in label_mappings.get(rm.from_label, []):
                    if cm.target_property in from_node.merge_keys:
                        formatted = repo.format_cypher_value(row.get(cm.source_column), "string")
                        if formatted is not None:
                            from_keys[cm.target_property] = formatted

                to_keys = {}
                for cm in label_mappings.get(rm.to_label, []):
                    if cm.target_property in to_node.merge_keys:
                        formatted = repo.format_cypher_value(row.get(cm.source_column), "string")
                        if formatted is not None:
                            to_keys[cm.target_property] = formatted

                if not from_keys or not to_keys:
                    continue

                prop_types = rel_prop_types.get(rm.rel_type, {})
                rel_props = {}
                for src_col, rel_prop in rm.properties.items():
                    dtype = prop_types.get(rel_prop, "string")
                    formatted = repo.format_cypher_value(row.get(src_col), dtype)
                    if formatted is not None:
                        rel_props[rel_prop] = formatted

                cypher = repo.build_merge_relationship_cypher(
                    rm.from_label, from_keys,
                    rm.to_label, to_keys,
                    rm.rel_type, rel_props, org_id,
                )
                try:
                    repo.execute_graph_merge(db, cypher)
                    stats.relationships_created += 1
                except Exception as e:
                    db.rollback()
                    stats.errors.append(f"관계 MERGE 실패 [{rm.rel_type}]: {e}")

        db.commit()

    return stats


# === 자연어 질의 ===

def _build_query_system_prompt(org_id: str, extended_property_hints: list[str]) -> str:
    """테넌트 격리 규칙 + 확장 속성 힌트 포함 시스템 프롬프트"""
    ext_section = ""
    if extended_property_hints:
        ext_list = "\n".join(f"  - {h}" for h in extended_property_hints)
        ext_section = f"""
## 확장 속성 (이 테넌트에서 사용 가능)
{ext_list}
확장 속성은 `_ext_` 프리픽스가 붙어 있으며, 일반 속성처럼 WHERE 절에서 사용 가능합니다.
"""

    return f"""당신은 Apache AGE (PostgreSQL 그래프 DB) Cypher 쿼리 생성 전문가입니다.
사용자의 자연어 질문을 Cypher 쿼리로 변환하세요.

{MANUFACTURING_ONTOLOGY.to_prompt_text()}
{ext_section}

## 테넌트 격리 규칙 (필수!)
- 모든 MATCH 절의 노드에 반드시 `_org_id: '{org_id}'` 조건을 포함하세요.
- 예: MATCH (p:Part {{_org_id: '{org_id}', part_number: 'PRT-001'}})

## 쿼리 규칙
1. MATCH 쿼리만 생성하세요. CREATE/MERGE/DELETE/SET은 절대 금지입니다.
2. 반드시 유효한 Cypher 문법을 사용하세요.
3. 결과는 Cypher 쿼리만 출력하세요 (설명 없이).
4. 노드 라벨과 관계 타입은 위에 정의된 것만 사용하세요.
"""


CYPHER_SYSTEM_PROMPT = f"""당신은 Apache AGE (PostgreSQL 그래프 DB) Cypher 쿼리 생성 전문가입니다.
사용자의 자연어 질문을 Cypher 쿼리로 변환하세요.

{MANUFACTURING_ONTOLOGY.to_prompt_text()}

## 규칙
1. MATCH 쿼리만 생성하세요. CREATE/MERGE/DELETE/SET은 절대 금지입니다.
2. 반드시 유효한 Cypher 문법을 사용하세요.
3. 결과는 Cypher 쿼리만 출력하세요 (설명 없이).
4. 노드 라벨과 관계 타입은 위에 정의된 것만 사용하세요.
5. 속성명은 정확히 위에 정의된 이름을 사용하세요.
"""


def natural_language_query(db: Session, question: str, org_id: str) -> QueryResponse:
    """자연어 질의 실행 (테넌트 격리)"""
    ext_hints = repo.get_extended_property_hints(db, org_id)
    system_prompt = _build_query_system_prompt(org_id, ext_hints)

    cypher = chat_completion(system_prompt=system_prompt, user_message=question)
    _validate_read_only(cypher)

    raw_results = repo.execute_graph_query(db, cypher)
    results = _serialize_results(raw_results)
    return QueryResponse(cypher_query=cypher, results=results)


def text_to_cypher(question: str) -> str:
    """자연어 질문을 Cypher 쿼리로 변환 (테넌트 격리 없음)"""
    return chat_completion(system_prompt=CYPHER_SYSTEM_PROMPT, user_message=question)


def execute_cypher_query(db: Session, cypher: str) -> list:
    """Cypher 쿼리 직접 실행"""
    raw = repo.execute_graph_query(db, cypher)
    return _serialize_results(raw)


def _validate_read_only(cypher: str):
    """읽기 전용 쿼리인지 검증"""
    upper = cypher.upper()
    for keyword in ("CREATE", "MERGE", "DELETE", "SET ", "REMOVE", "DROP"):
        if keyword in upper:
            raise ValueError(f"데이터 변경 쿼리는 허용되지 않습니다: {keyword}")


def _serialize_results(raw_results: list) -> list[dict]:
    """AGE 결과를 JSON 직렬화 가능한 형태로 변환"""
    results = []
    for r in raw_results:
        if isinstance(r, dict):
            results.append({k: str(v) for k, v in r.items()})
        else:
            results.append({"result": str(r)})
    return results
