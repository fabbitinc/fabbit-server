package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.common.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.dto.common.RelationMappingDto;
import com.fabbitinc.server.application.mapping.dto.response.MappingImpactSummaryResponse;
import com.fabbitinc.server.application.mapping.dto.response.ValidationIssueResponse;
import com.fabbitinc.server.application.mapping.dto.response.ValidationSeverity;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MappingValidationSupport {

    public ValidationResult validateAgainstRows(
            List<String> headers,
            List<Map<String, Object>> sampleRows,
            MappingResultDto mapping
    ) {
        Set<String> headerSet = new LinkedHashSet<>(headers);
        List<ValidationIssueResponse> errors = new ArrayList<>();
        List<ValidationIssueResponse> warnings = new ArrayList<>();

        Set<RelationshipType> validRelTypes = new LinkedHashSet<>();
        Map<String, Set<String>> mergeKeysByLabel = ManufacturingOntology.ONTOLOGY.nodeLabels().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ManufacturingOntology.NodeLabelDef::label,
                        nodeLabel -> new LinkedHashSet<>(nodeLabel.mergeKeys())
                ));
        ManufacturingOntology.ONTOLOGY.relationshipTypes().forEach(relationship -> validRelTypes.add(relationship.relType()));

        validatePropertyMappings(mapping.propertyMappings(), headerSet, sampleRows, errors, warnings);
        validateRelationMappings(mapping.relationMappings(), headerSet, sampleRows, validRelTypes, mergeKeysByLabel, errors, warnings);

        Set<String> usedColumns = new LinkedHashSet<>();
        mapping.propertyMappings().forEach(property -> usedColumns.add(property.sourceColumn()));
        mapping.relationMappings().forEach(relation -> {
            usedColumns.addAll(relation.nodeColumns().values());
            usedColumns.addAll(relation.relColumns().values());
        });

        int disabledCount = 0;
        for (String header : headers) {
            if (!usedColumns.contains(header)) {
                disabledCount++;
            }
        }

        return new ValidationResult(
                errors,
                warnings,
                new MappingImpactSummaryResponse(disabledCount)
        );
    }

    private void validatePropertyMappings(
            List<PropertyMappingDto> properties,
            Set<String> headerSet,
            List<Map<String, Object>> sampleRows,
            List<ValidationIssueResponse> errors,
            List<ValidationIssueResponse> warnings
    ) {
        for (int index = 0; index < properties.size(); index++) {
            PropertyMappingDto property = properties.get(index);
            if (property.sourceColumn() == null || !headerSet.contains(property.sourceColumn())) {
                errors.add(new ValidationIssueResponse(
                        "MISSING_SOURCE_COLUMN",
                        ValidationSeverity.ERROR,
                        "컬럼 '" + property.sourceColumn() + "'을(를) 파일에서 찾을 수 없습니다",
                        "property_mappings[" + index + "].source_column",
                        "missing_source_column"
                ));
                continue;
            }

            if (isNumericType(property.dataType()) && hasNonNumericSample(sampleRows, property.sourceColumn())) {
                warnings.add(new ValidationIssueResponse(
                        "NUMERIC_PARSE_WARNING",
                        ValidationSeverity.WARNING,
                        "컬럼 '" + property.sourceColumn() + "'에 숫자로 해석하기 어려운 값이 있습니다",
                        "property_mappings[" + index + "].data_type",
                        null
                ));
            }
        }
    }

    private void validateRelationMappings(
            List<RelationMappingDto> relations,
            Set<String> headerSet,
            List<Map<String, Object>> sampleRows,
            Set<RelationshipType> validRelTypes,
            Map<String, Set<String>> mergeKeysByLabel,
            List<ValidationIssueResponse> errors,
            List<ValidationIssueResponse> warnings
    ) {
        for (int index = 0; index < relations.size(); index++) {
            RelationMappingDto relation = relations.get(index);
            if (!validRelTypes.contains(relation.relType())) {
                errors.add(new ValidationIssueResponse(
                        "INVALID_REL_TYPE",
                        ValidationSeverity.ERROR,
                        "허용되지 않은 관계 타입입니다: " + relation.relType(),
                        "relation_mappings[" + index + "].rel_type",
                        null
                ));
                continue;
            }

            boolean rootless = relation.nodeColumns().isEmpty() && !relation.relColumns().isEmpty();
            if (!rootless) {
                Set<String> requiredKeys = mergeKeysByLabel.getOrDefault(relation.targetLabel(), Set.of());
                for (String mergeKey : requiredKeys) {
                    String sourceColumn = relation.nodeColumns().get(mergeKey);
                    if (sourceColumn == null || sourceColumn.isBlank()) {
                        errors.add(new ValidationIssueResponse(
                                "MISSING_NODE_MERGE_KEY",
                                ValidationSeverity.ERROR,
                                "관계 '" + relation.relType() + "'의 대상 노드 merge key '" + mergeKey + "' 매핑이 누락되었습니다",
                                "relation_mappings[" + index + "].node_columns." + mergeKey,
                                "missing_node_merge_key"
                        ));
                        continue;
                    }

                    if (!headerSet.contains(sourceColumn)) {
                        errors.add(new ValidationIssueResponse(
                                "MISSING_SOURCE_COLUMN",
                                ValidationSeverity.ERROR,
                                "컬럼 '" + sourceColumn + "'을(를) 파일에서 찾을 수 없습니다",
                                "relation_mappings[" + index + "].node_columns." + mergeKey,
                                "missing_source_column"
                        ));
                    }
                }
            }

            for (Map.Entry<String, String> relProperty : relation.relColumns().entrySet()) {
                String propertyName = relProperty.getKey();
                String sourceColumn = relProperty.getValue();
                String path = "relation_mappings[" + index + "].rel_columns." + propertyName;

                if (!headerSet.contains(sourceColumn)) {
                    errors.add(new ValidationIssueResponse(
                            "MISSING_SOURCE_COLUMN",
                            ValidationSeverity.ERROR,
                            "관계 속성 컬럼 '" + sourceColumn + "'을(를) 파일에서 찾을 수 없습니다",
                            path,
                            "missing_source_column"
                    ));
                    continue;
                }

                PropertyDataType dataType = relation.relColumnTypes().getOrDefault(propertyName, PropertyDataType.STRING);
                if (isNumericType(dataType) && hasNonNumericSample(sampleRows, sourceColumn)) {
                    warnings.add(new ValidationIssueResponse(
                            "NUMERIC_PARSE_WARNING",
                            ValidationSeverity.WARNING,
                            "관계 속성 컬럼 '" + sourceColumn + "'에 숫자로 해석하기 어려운 값이 있습니다",
                            "relation_mappings[" + index + "].rel_column_types." + propertyName,
                            null
                    ));
                }
            }
        }
    }

    private boolean isNumericType(PropertyDataType dataType) {
        return dataType == PropertyDataType.INTEGER || dataType == PropertyDataType.FLOAT;
    }

    private boolean hasNonNumericSample(List<Map<String, Object>> sampleRows, String sourceColumn) {
        for (Map<String, Object> sampleRow : sampleRows) {
            Object value = sampleRow.get(sourceColumn);
            if (value == null) {
                continue;
            }

            String text = String.valueOf(value).trim();
            if (text.isBlank()) {
                continue;
            }

            if (!canParseNumeric(text)) {
                return true;
            }
        }
        return false;
    }

    private boolean canParseNumeric(String raw) {
        String cleaned = raw.replace(",", "").trim();
        try {
            Double.parseDouble(cleaned);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public record ValidationResult(
            List<ValidationIssueResponse> errors,
            List<ValidationIssueResponse> warnings,
            MappingImpactSummaryResponse impactSummary
    ) {
    }
}
