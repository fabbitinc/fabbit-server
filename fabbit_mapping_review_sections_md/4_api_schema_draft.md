# 4. API 응답 스키마 초안(candidate suggestions 포함)

목표: validate 응답이 **issues + suggestions + patch**를 같이 반환하여, UI가 즉시 복구 동선을 제공하게 합니다.

## 4-1. validate 응답 JSON 예시

```json
{
  "job_id": "job_123",
  "conforms": false,
  "normalized_mapping": {
    "column_mappings": {
      "품번": {
        "target": {"kind": "node_property", "label": "Part", "property": "part_no_norm"},
        "type": "string"
      },
      "_ext_unit_price_1": {
        "target": {"kind": "ext_property", "scope": "Part"},
        "type": "float"
      }
    },
    "relation_mappings": {
      "SUPPLIED_BY": {
        "rel_type": "SUPPLIED_BY",
        "from_label": "Part",
        "to_label": "Supplier",
        "state": "INCOMPLETE",
        "from_columns": {"part_key": "품번"},
        "to_columns": {},
        "properties": {},
        "property_types": {"unit_cost": "float"}
      }
    }
  },
  "issues": [
    {
      "id": "iss_001",
      "severity": "ERROR",
      "code": "REL_ENDPOINT_MISSING",
      "message": "SUPPLIED_BY is INCOMPLETE: missing Supplier endpoint columns (to_columns).",
      "path": "/relation_mappings/SUPPLIED_BY/to_columns",
      "related_columns": ["단가(원)"],
      "suggestion_ids": ["sug_201"]
    },
    {
      "id": "iss_010",
      "severity": "WARNING",
      "code": "EXT_HAS_STRONG_ONTOLOGY_CANDIDATE",
      "message": "Column '단가(원)' is mapped to _ext_* but has strong candidates in ontology relation properties.",
      "path": "/column_mappings/_ext_unit_price_1",
      "related_columns": ["단가(원)"],
      "suggestion_ids": ["sug_201"]
    }
  ],
  "suggestions": [
    {
      "id": "sug_201",
      "source_column": "단가(원)",
      "target": {
        "kind": "edge_property",
        "rel_type": "SUPPLIED_BY",
        "from_label": "Part",
        "to_label": "Supplier",
        "property": "unit_cost",
        "property_type": "float"
      },
      "score": 0.86,
      "confidence": "MEDIUM",
      "rationale": [
        "header contains token '단가'",
        "unit hint '(원)' detected",
        "sample values are numeric"
      ],
      "blockers": [
        {
          "type": "MISSING_ENDPOINT_MAPPING",
          "rel_type": "SUPPLIED_BY",
          "missing": ["to_columns.supplier_key"],
          "candidate_columns": ["공급처", "업체명", "vendor_code"]
        }
      ],
      "patch": [
        {
          "op": "replace",
          "path": "/relation_mappings/SUPPLIED_BY/properties/unit_cost",
          "value": {"source_column": "단가(원)", "transform": "to_float"}
        },
        {
          "op": "replace",
          "path": "/column_mappings/_ext_unit_price_1",
          "value": null
        }
      ],
      "risk_notes": [
        "단가 의미가 '공급단가'가 아닐 수 있습니다. 사용자가 의미를 확정해야 합니다."
      ]
    }
  ]
}
```

## 4-2. 설계 포인트
- `relation_mappings`는 `rel_type`을 key로 하는 dict가 patch path 안정성이 좋습니다.
- `suggestions[].patch`는 서버에서 dry-run 적용 후 validate까지 통과 가능한 경우만 제공하는 편이 안전합니다.
- blockers는 “왜 지금 당장 못 붙이는지”를 UI가 명확히 안내할 수 있게 합니다.

## 4-3. patch 포맷 참고
- JSON Patch는 RFC 6902 표준입니다.
  - https://datatracker.ietf.org/doc/html/rfc6902
