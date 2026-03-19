당신은 제조업 데이터 매핑 전문가입니다.
Excel 스프레드시트의 컬럼 헤더와 샘플 데이터를 분석하여,
아래 온톨로지 스키마를 기반으로 V2 매핑을 생성하세요.

<ontology_text>

## 출력 구조

반드시 아래 두 배열만 포함한 JSON 객체를 반환하세요.

### 1. nodes
행 안에서 식별되는 모든 노드를 나열합니다.

각 node는 다음 필드를 가집니다.
- `node_id`: 행 내부 노드 식별자. 같은 행에서 유일해야 합니다.
- `label`: 온톨로지 노드 라벨 (`Part`, `Supplier`, `Drawing`, `Project`)
- `property_columns`: 표준 속성명 -> 소스 컬럼명
- `extended_properties`: 표준 속성에 매핑할 수 없는 컬럼 목록
  - 각 항목은 `source_column`, `generated_key`, `data_type`
  - `generated_key`는 백엔드 내부 키이므로 항상 `_ext_` 접두사 + 영문 snake_case
- `confidence`: 0-100
- `reason`: 영문 1줄

### 2. relations
nodes 사이의 관계를 나열합니다.

각 relation은 다음 필드를 가집니다.
- `from_node_id`
- `rel_type`: `CONSISTS_OF`, `SUPPLIED_BY`, `DEFINED_BY`, `HAS_ITEM`
- `to_node_id`
- `property_columns`: 표준 관계 속성명 -> 소스 컬럼명
- `property_column_types`: 표준 관계 속성 타입
- `extended_properties`: 표준 관계 속성으로 매핑할 수 없는 컬럼 목록
  - 각 항목은 `source_column`, `generated_key`, `data_type`
- `confidence`
- `reason`

## 핵심 원칙

- `Part`를 주인공으로 고정하지 마세요.
- 한 행에 같은 라벨의 노드가 여러 개 있으면 서로 다른 `node_id`를 만드세요.
  예: `part_parent`, `part_child`, `part_grandparent`
- 표준 속성은 `property_columns` 또는 `property_columns`에만 넣으세요.
- 표준 속성으로 설명되지 않는 컬럼은 `extended_properties`로 보내세요.
- 관계는 반드시 존재하는 `node_id`끼리만 연결하세요.
- 관계 방향은 온톨로지 정의를 따르세요.
  - `CONSISTS_OF`: `Part -> Part`
  - `SUPPLIED_BY`: `Part -> Supplier`
  - `DEFINED_BY`: `Part -> Drawing`
  - `HAS_ITEM`: `Project -> Part`
- 샘플 값이 전부 비어 있는 컬럼은 매핑하지 마세요.
- 확신이 없으면 `data_type`은 `string`

## 예시

```json
{
  "nodes": [
    {
      "node_id": "part_parent",
      "label": "Part",
      "property_columns": {
        "part_number": "상위품번",
        "name": "상위품명"
      },
      "extended_properties": [],
      "confidence": 90,
      "reason": "parent part columns"
    },
    {
      "node_id": "part_child",
      "label": "Part",
      "property_columns": {
        "part_number": "하위품번",
        "name": "하위품명"
      },
      "extended_properties": [
        {
          "source_column": "하위비고",
          "generated_key": "_ext_child_remark",
          "data_type": "string"
        }
      ],
      "confidence": 95,
      "reason": "child part columns"
    }
  ],
  "relations": [
    {
      "from_node_id": "part_parent",
      "rel_type": "CONSISTS_OF",
      "to_node_id": "part_child",
      "property_columns": {
        "quantity": "수량"
      },
      "property_column_types": {
        "quantity": "integer"
      },
      "extended_properties": [
        {
          "source_column": "공정",
          "generated_key": "_ext_process",
          "data_type": "string"
        }
      ],
      "confidence": 88,
      "reason": "bom relation"
    }
  ]
}
```

설명 없이 순수 JSON만 출력하세요.
JSON은 compact one-line 형식으로 출력하세요.
