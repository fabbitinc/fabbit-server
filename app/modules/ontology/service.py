"""온톨로지 비즈니스 로직.

LLM 매핑 생성 + 검증을 담당합니다.
"""

import json

from app.infrastructure.llm_client import LLMResponse, chat_completion_with_usage
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import (
    ColumnMapping,
    ExtendedPropertyMapping,
    MappingResult,
    RelationMapping,
)


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

## 신뢰도 규칙
- 각 매핑에 confidence (0-100 정수)와 reason (영문 1줄)을 반드시 포함하세요.
- confidence: 매핑이 정확하다는 확신 정도 (100=확실, 50=불확실, 0=추측)
- reason: 해당 매핑을 선택한 근거를 영문 1줄로 설명

## 출력 형식 (JSON)
```json
{{
  "column_mappings": [
    {{"source_column": "품번", "target_label": "Part", "target_property": "part_number", "data_type": "string", "confidence": 95, "reason": "Column header directly translates to part number"}}
  ],
  "relation_mappings": [
    {{"from_label": "Part", "to_label": "Material", "rel_type": "MADE_OF", "properties": {{}}, "property_types": {{}}}}
  ],
  "extended_properties": [
    {{"source_column": "탄소배출량", "target_label": "Part", "property_name": "_ext_carbon_emission", "data_type": "float", "confidence": 80, "reason": "Not in ontology but clearly carbon emission data"}}
  ]
}}
```

반드시 위 JSON 형식만 출력하세요. 설명이나 마크다운 없이 순수 JSON만 출력하세요.
"""


def generate_mapping(
    headers: list[str], sample_rows: list[dict]
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

    llm_resp = chat_completion_with_usage(
        system_prompt=MAPPING_SYSTEM_PROMPT,
        user_message=user_message,
        response_format={"type": "json_object"},
    )
    raw = json.loads(llm_resp.content)

    result = MappingResult(
        column_mappings=[ColumnMapping(**cm) for cm in raw.get("column_mappings", [])],
        relation_mappings=[RelationMapping(**rm) for rm in raw.get("relation_mappings", [])],
        extended_properties=[ExtendedPropertyMapping(**ep) for ep in raw.get("extended_properties", [])],
    )

    return _validate_and_fix_mapping(result), llm_resp


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
