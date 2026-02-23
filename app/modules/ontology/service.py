"""온톨로지 비즈니스 로직.

LLM 매핑 생성 + 검증을 담당합니다.
"""

import json
import re

from sqlalchemy.orm import Session

from app.core.exceptions import AppError
from app.core.transactional import transactional
from app.infrastructure.llm_client import LLMModel, LLMResponse, chat_completion_with_usage
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import (
    MappingResult,
    NodeLabelSchema,
    NodeSearchItem,
    NodeSearchResponse,
    OntologySchemaResponse,
    PropertyMapping,
    PropertySchema,
    RelationMapping,
    RelationshipTypeSchema,
)

_EXT_NAME_RE = re.compile(r"^_ext_[a-z0-9_]+$")

# 라벨 → repository 매핑 (lazy import 방지용 dict)
_LABEL_SEARCH_REPOS: dict[str, str] = {
    "Part": "app.modules.part.repository",
    "Drawing": "app.modules.drawing.repository",
    "Supplier": "app.modules.supplier.repository",
    "Project": "app.modules.project.repository",
}

# === 온톨로지 스키마 조회 ===

_cached_schema: OntologySchemaResponse | None = None


def get_ontology_schema() -> OntologySchemaResponse:
    """온톨로지 스키마 조회 (정적 데이터, 1회 빌드 후 캐싱)."""
    global _cached_schema
    if _cached_schema is not None:
        return _cached_schema

    ont = MANUFACTURING_ONTOLOGY

    node_labels = []
    for nl in ont.node_labels:
        props = [
            PropertySchema(
                name=p.name,
                description=p.description,
                data_type=p.data_type,
                required=p.required,
                is_merge_key=p.is_merge_key,
            )
            for p in nl.properties
        ]
        node_labels.append(
            NodeLabelSchema(
                label=nl.label,
                description=nl.description,
                properties=props,
                merge_keys=nl.merge_keys,
            )
        )

    relationship_types = []
    for rt in ont.relationship_types:
        props = [
            PropertySchema(
                name=p.name,
                description=p.description,
                data_type=p.data_type,
                required=p.required,
                is_merge_key=p.is_merge_key,
            )
            for p in rt.properties
        ]
        relationship_types.append(
            RelationshipTypeSchema(
                rel_type=rt.rel_type,
                description=rt.description,
                from_label=rt.from_label,
                to_label=rt.to_label,
                properties=props,
            )
        )

    _cached_schema = OntologySchemaResponse(
        name=ont.name,
        description=ont.description,
        node_labels=node_labels,
        relationship_types=relationship_types,
    )
    return _cached_schema


# === 노드 merge key 검색 ===


@transactional(read_only=True)
def search_nodes(
    db: Session,
    label: str,
    search: str,
    limit: int = 10,
) -> NodeSearchResponse:
    """노드 라벨별 merge key 검색 (RDS)."""
    if label not in _LABEL_SEARCH_REPOS:
        raise AppError(
            message=f"지원하지 않는 노드 라벨입니다: {label}",
            code="INVALID_LABEL",
        )

    import importlib

    repo_module = importlib.import_module(_LABEL_SEARCH_REPOS[label])
    rows = repo_module.search_merge_key(db, search, limit)

    items = [NodeSearchItem(value=r["value"], label=r["label"]) for r in rows]
    return NodeSearchResponse(node_label=label, items=items)


# === 매핑 생성 ===

MAPPING_SYSTEM_PROMPT = f"""당신은 제조업 데이터 매핑 전문가입니다.
Excel 스프레드시트의 컬럼 헤더와 샘플 데이터를 분석하여,
아래 온톨로지 스키마에 매핑하세요.

{MANUFACTURING_ONTOLOGY.to_mapping_prompt_text()}

## 매핑 구조

매핑 결과는 두 가지 카테고리로 구분합니다:

### 1. property_mappings — Part 속성 매핑
행의 주인공(대상 Part)에 귀속되는 속성입니다.
- 품번(part_number), 품명(name), 재질(material), 단위(unit) 등
- 온톨로지에 없는 추가 속성은 `_ext_` 접두사 + 영문 snake_case로 작성
  예: "탄소배출량" → "_ext_carbon_emission"
- **관계에 정의된 속성(quantity, unit_cost 등)은 여기에 넣지 마세요.** 반드시 relation_mappings의 rel_columns에 매핑하세요.

### 2. relation_mappings — 외부 관계 매핑
행의 주인공 Part와 다른 노드(상위 Part, Supplier, Drawing 등)의 관계입니다.
각 관계에는:
- `rel_type`: 온톨로지 관계 타입 (CONSISTS_OF, SUPPLIED_BY, DEFINED_BY, HAS_ITEM)
- `target_label`: 상대방 노드 라벨
- `node_columns`: 상대방 노드의 속성 → 소스 컬럼 매핑
- `rel_columns`: 관계 자체의 속성 → 소스 컬럼 매핑
- `rel_column_types`: 관계 속성의 데이터 타입

## 관계 매핑 원칙

아래 4가지 원칙을 순서대로 적용하세요. 관계별 하드코딩된 규칙 대신 온톨로지 스키마를 참조합니다.

### 원칙 1. 온톨로지 스키마 참조
위 "관계 타입" 섹션에서 각 관계의 **방향**, **대상 노드 MERGE KEY**, **관계 속성**을 확인하세요.
- `node_columns`에는 대상 노드의 속성(특히 MERGE KEY)을 매핑
- `rel_columns`에는 해당 관계에 정의된 속성만 매핑

### 원칙 2. 관계 속성은 반드시 rel_columns에
온톨로지에서 관계 속성으로 정의된 컬럼(예: quantity → CONSISTS_OF, unit_cost → SUPPLIED_BY)은
**절대 property_mappings에 넣지 말고** 해당 관계의 `rel_columns`에 매핑하세요.

### 원칙 3. Rootless relation — 대상 노드 컬럼이 없어도 관계 생성
데이터에 **관계 속성에 해당하는 컬럼은 있지만** 대상 노드를 식별할 컬럼이 없는 경우:
- `node_columns`: {{}} (빈 딕셔너리)
- `rel_columns`: 관계 속성만 매핑
- 예: 수량 컬럼은 있지만 상위품번 컬럼이 없는 경우 → CONSISTS_OF rootless
  `{{"rel_type": "CONSISTS_OF", "target_label": "Part", "node_columns": {{}}, "rel_columns": {{"quantity": "수량"}}, "rel_column_types": {{"quantity": "integer"}}}}`
- 예: 단가 컬럼은 있지만 공급업체 컬럼이 없는 경우 → SUPPLIED_BY rootless
  `{{"rel_type": "SUPPLIED_BY", "target_label": "Supplier", "node_columns": {{}}, "rel_columns": {{"unit_cost": "단가"}}, "rel_column_types": {{"unit_cost": "float"}}}}`

### 원칙 4. 관계 생성 기준
관계 속성(`rel_columns`) 또는 대상 노드 컬럼(`node_columns`)이 **하나라도** 매핑 가능하면 해당 관계를 생성하세요.
둘 다 매핑할 컬럼이 없으면 관계를 생성하지 마세요.

## 품질 가드레일
- 샘플 행 기준으로 **값이 전부 비어 있는 컬럼은 절대 매핑하지 마세요**.
- `_ext_` 접두사는 **한 번만** 사용하세요 (중복 금지).
- 매핑할 수 없는 컬럼(빈 값, 의미 없는 인덱스 등)은 무시합니다.

## 데이터 타입
- "string", "integer", "float", "boolean" 중 선택
- 확신이 없으면 "string"으로 지정하세요.

## 신뢰도
- confidence (0-100 정수): 매핑 정확도 확신 수준
- reason (영문 1줄): 해당 매핑을 선택한 근거

## 출력 형식 (JSON)
```json
{{
  "property_mappings": [
    {{"source_column": "품번", "target_property": "part_number", "data_type": "string", "confidence": 95, "reason": "Column header directly translates to part number"}}
  ],
  "relation_mappings": [
    {{"rel_type": "SUPPLIED_BY", "target_label": "Supplier", "node_columns": {{"company_name": "업체명"}}, "rel_columns": {{}}, "rel_column_types": {{}}, "confidence": 85, "reason": "Supplier column maps to SUPPLIED_BY relationship"}},
    {{"rel_type": "CONSISTS_OF", "target_label": "Part", "node_columns": {{}}, "rel_columns": {{"quantity": "수량"}}, "rel_column_types": {{"quantity": "integer"}}, "confidence": 80, "reason": "Quantity column without parent part number - rootless BOM relation"}}
  ]
}}
```

반드시 위 JSON 형식만 출력하세요. 설명이나 마크다운 없이 순수 JSON만 출력하세요.
"""


def generate_mapping(
    headers: list[str],
    sample_rows: list[dict],
    model: LLMModel | None = None,
) -> tuple[MappingResult, LLMResponse]:
    """Excel 헤더 + 샘플 데이터로 매핑 생성 (LLM 1회 호출 + 검증).

    Returns:
        (MappingResult, LLMResponse) — 매핑 결과 + LLM 토큰 사용량
    """
    user_message = f"""다음 Excel 데이터를 분석하여 매핑하세요.

## 컬럼 헤더
{json.dumps(headers, ensure_ascii=False)}

## 샘플 데이터 (처음 5행)
{json.dumps(sample_rows, ensure_ascii=False, indent=2)}
"""

    kwargs = dict(
        system_prompt=MAPPING_SYSTEM_PROMPT,
        user_message=user_message,
        max_tokens=2000,
        response_format={"type": "json_object"},
    )
    if model is not None:
        kwargs["model"] = model
    llm_resp = chat_completion_with_usage(**kwargs)
    raw = json.loads(llm_resp.content)

    # LLM이 배열로 감싸서 반환하는 경우 언래핑
    if isinstance(raw, list):
        if len(raw) == 1 and isinstance(raw[0], dict):
            raw = raw[0]
        else:
            raise AppError("LLM이 올바른 JSON 형식을 반환하지 않았습니다")
    if not isinstance(raw, dict):
        raise AppError("LLM이 올바른 JSON 형식을 반환하지 않았습니다")

    result = MappingResult(
        property_mappings=[
            PropertyMapping(**pm) for pm in raw.get("property_mappings", [])
        ],
        relation_mappings=[
            RelationMapping(**rm) for rm in raw.get("relation_mappings", [])
        ],
    )

    return _validate_and_fix_mapping(result), llm_resp


def normalize_mapping(mapping: MappingResult) -> MappingResult:
    """매핑 결과를 온톨로지 규칙에 맞게 정규화."""

    return _validate_and_fix_mapping(mapping)


def _normalize_ext_property_name(name: str) -> str:
    normalized = (name or "").strip()
    while normalized.startswith("_ext__ext_"):
        normalized = normalized[len("_ext_") :]
    if normalized.startswith("_ext_"):
        core = normalized[len("_ext_") :]
    else:
        core = normalized
    core = core.strip("_")
    return f"_ext_{core}" if core else "_ext_unknown"


def _fix_rel_column_types(rm: RelationMapping) -> RelationMapping:
    """rel_column_types 누락 시 온톨로지 관계 속성 정의에서 자동 보정"""
    if not rm.rel_columns:
        return rm
    rel_def = MANUFACTURING_ONTOLOGY.get_relationship_type(rm.rel_type)
    if rel_def is None:
        return rm
    # 온톨로지에 정의된 관계 속성의 타입 맵
    ontology_types = {p.name: p.data_type for p in rel_def.properties}
    fixed_types = dict(rm.rel_column_types)
    changed = False
    for rel_key in rm.rel_columns:
        if rel_key not in fixed_types and rel_key in ontology_types:
            fixed_types[rel_key] = ontology_types[rel_key]
            changed = True
    if not changed:
        return rm
    return rm.model_copy(update={"rel_column_types": fixed_types})


def _validate_and_fix_mapping(result: MappingResult) -> MappingResult:
    """온톨로지 라벨/속성 유효성 검증 + 자동 보정"""
    valid_labels = MANUFACTURING_ONTOLOGY.get_valid_labels()
    valid_rel_types = MANUFACTURING_ONTOLOGY.get_valid_rel_types()

    # Part 속성 검증
    part_node = MANUFACTURING_ONTOLOGY.get_node_label("Part")
    valid_part_props = {p.name for p in part_node.properties} if part_node else set()

    verified_props = []
    for pm in result.property_mappings:
        if pm.target_property.startswith("_ext_"):
            # 확장 속성: 이름 정규화
            normalized_name = _normalize_ext_property_name(pm.target_property)
            verified_props.append(
                PropertyMapping(
                    source_column=pm.source_column,
                    target_property=normalized_name,
                    data_type=pm.data_type,
                    confidence=pm.confidence,
                    reason=pm.reason,
                    is_extended=True,
                )
            )
        elif pm.target_property in valid_part_props:
            # 표준 속성: is_extended=False 보장
            verified_props.append(pm.model_copy(update={"is_extended": False}))
        else:
            # 온톨로지에 없는 속성 → 확장 속성으로 변환
            verified_props.append(
                PropertyMapping(
                    source_column=pm.source_column,
                    target_property=f"_ext_{pm.target_property}",
                    data_type=pm.data_type,
                    confidence=pm.confidence,
                    reason=pm.reason,
                    is_extended=True,
                )
            )

    # 관계 매핑 검증
    verified_rels = []
    for rm in result.relation_mappings:
        if rm.rel_type not in valid_rel_types:
            continue
        if rm.target_label not in valid_labels:
            continue
        # Rootless relation: node_columns 없이 rel_columns만 있는 관계 허용
        if not rm.node_columns and rm.rel_columns:
            rm = _fix_rel_column_types(rm)
            verified_rels.append(rm)
            continue
        if not rm.node_columns:
            continue

        # merge key 검증: 상대방 노드의 merge key가 node_columns에 포함되어야 함
        target_node = MANUFACTURING_ONTOLOGY.get_node_label(rm.target_label)
        if target_node is None:
            continue
        has_merge_key = any(mk in rm.node_columns for mk in target_node.merge_keys)
        if not has_merge_key:
            continue

        rm = _fix_rel_column_types(rm)
        verified_rels.append(rm)

    # 중복 제거
    seen_props: set[tuple[str, str]] = set()
    deduped_props = []
    for pm in verified_props:
        key = (pm.source_column, pm.target_property)
        if key in seen_props:
            continue
        seen_props.add(key)
        deduped_props.append(pm)

    return MappingResult(
        property_mappings=deduped_props,
        relation_mappings=verified_rels,
    )
