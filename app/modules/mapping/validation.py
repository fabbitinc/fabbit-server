from app.modules.mapping.schemas import MappingImpactSummary, ValidationIssue
from app.modules.ontology.base_ontology import MANUFACTURING_ONTOLOGY
from app.modules.ontology.schemas import MappingResult


def validate_mapping_against_rows(
    headers: list[str],
    sample_rows: list[dict[str, object]],
    mapping: MappingResult,
) -> tuple[list[ValidationIssue], list[ValidationIssue], MappingImpactSummary]:
    header_set = set(headers)
    errors: list[ValidationIssue] = []
    warnings: list[ValidationIssue] = []

    merge_keys_by_label = {
        nl.label: set(nl.merge_keys) for nl in MANUFACTURING_ONTOLOGY.node_labels
    }

    for idx, prop_mapping in enumerate(mapping.property_mappings):
        if prop_mapping.source_column not in header_set:
            errors.append(
                ValidationIssue(
                    code="MISSING_SOURCE_COLUMN",
                    severity="error",
                    message=f"컬럼 '{prop_mapping.source_column}'을(를) 파일에서 찾을 수 없습니다",
                    path=f"property_mappings[{idx}].source_column",
                    dismissed_reason="missing_source_column",
                )
            )
            continue

        if prop_mapping.data_type in ("integer", "float") and _has_non_numeric_sample(
            sample_rows,
            prop_mapping.source_column,
        ):
            warnings.append(
                ValidationIssue(
                    code="NUMERIC_PARSE_WARNING",
                    severity="warning",
                    message=(
                        f"컬럼 '{prop_mapping.source_column}'에 숫자로 해석하기 어려운 값이 있습니다"
                    ),
                    path=f"property_mappings[{idx}].data_type",
                )
            )

    rel_defs = {rt.rel_type: rt for rt in MANUFACTURING_ONTOLOGY.relationship_types}
    for idx, rel_mapping in enumerate(mapping.relation_mappings):
        rel_def = rel_defs.get(rel_mapping.rel_type)
        if rel_def is None:
            errors.append(
                ValidationIssue(
                    code="INVALID_REL_TYPE",
                    severity="error",
                    message=f"허용되지 않은 관계 타입입니다: {rel_mapping.rel_type}",
                    path=f"relation_mappings[{idx}].rel_type",
                )
            )
            continue

        is_rootless = not rel_mapping.node_columns and rel_mapping.rel_columns
        if not is_rootless:
            required_keys = merge_keys_by_label.get(rel_mapping.target_label, set())
            for merge_key in required_keys:
                src_col = rel_mapping.node_columns.get(merge_key)
                if not src_col:
                    errors.append(
                        ValidationIssue(
                            code="MISSING_NODE_MERGE_KEY",
                            severity="error",
                            message=(
                                f"관계 '{rel_mapping.rel_type}'의 대상 노드 merge key "
                                f"'{merge_key}' 매핑이 누락되었습니다"
                            ),
                            path=f"relation_mappings[{idx}].node_columns.{merge_key}",
                            dismissed_reason="missing_node_merge_key",
                        )
                    )
                    continue

                if src_col not in header_set:
                    errors.append(
                        ValidationIssue(
                            code="MISSING_SOURCE_COLUMN",
                            severity="error",
                            message=f"컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다",
                            path=f"relation_mappings[{idx}].node_columns.{merge_key}",
                            dismissed_reason="missing_source_column",
                        )
                    )

        for rel_prop, src_col in rel_mapping.rel_columns.items():
            path = f"relation_mappings[{idx}].rel_columns.{rel_prop}"
            if src_col not in header_set:
                errors.append(
                    ValidationIssue(
                        code="MISSING_SOURCE_COLUMN",
                        severity="error",
                        message=f"관계 속성 컬럼 '{src_col}'을(를) 파일에서 찾을 수 없습니다",
                        path=path,
                        dismissed_reason="missing_source_column",
                    )
                )
                continue

            data_type = rel_mapping.rel_column_types.get(rel_prop, "string")
            if data_type in ("integer", "float") and _has_non_numeric_sample(
                sample_rows,
                src_col,
            ):
                warnings.append(
                    ValidationIssue(
                        code="NUMERIC_PARSE_WARNING",
                        severity="warning",
                        message=(
                            f"관계 속성 컬럼 '{src_col}'에 숫자로 해석하기 어려운 값이 있습니다"
                        ),
                        path=f"relation_mappings[{idx}].rel_column_types.{rel_prop}",
                    )
                )

    used_columns = {pm.source_column for pm in mapping.property_mappings}
    for rel_mapping in mapping.relation_mappings:
        used_columns.update(rel_mapping.node_columns.values())
        used_columns.update(rel_mapping.rel_columns.values())

    impact_summary = MappingImpactSummary(
        disabled_column_count=sum(1 for h in headers if h not in used_columns),
    )
    return errors, warnings, impact_summary


def _has_non_numeric_sample(
    sample_rows: list[dict[str, object]], source_column: str
) -> bool:
    values = [row.get(source_column) for row in sample_rows]
    for value in values:
        if value is None:
            continue

        text = str(value).strip()
        if not text:
            continue

        if _can_parse_numeric(text):
            continue

        return True

    return False


def _can_parse_numeric(value: str) -> bool:
    cleaned = value.replace(",", "").strip()
    try:
        float(cleaned)
        return True
    except ValueError:
        return False
