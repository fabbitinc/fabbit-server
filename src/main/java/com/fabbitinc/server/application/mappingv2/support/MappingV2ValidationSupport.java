package com.fabbitinc.server.application.mappingv2.support;

import com.fabbitinc.server.application.mappingv2.dto.common.ExtendedPropertyMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.MappingV2ResultDto;
import com.fabbitinc.server.application.mappingv2.dto.common.NodeMappingV2Dto;
import com.fabbitinc.server.application.mappingv2.dto.common.RelationMappingV2Dto;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MappingV2ValidationSupport {

    public ValidationResult validateAgainstRows(
            List<String> headers,
            List<Map<String, Object>> sampleRows,
            MappingV2ResultDto mapping
    ) {
        Set<String> headerSet = new LinkedHashSet<>(headers);
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        Map<String, Map<String, PropertyDataType>> nodePropertyTypesByLabel = new LinkedHashMap<>();
        ManufacturingOntology.ONTOLOGY.nodeLabels().forEach(node -> {
            Map<String, PropertyDataType> propertyTypes = new LinkedHashMap<>();
            node.properties().forEach(property -> propertyTypes.put(property.name(), property.dataType()));
            nodePropertyTypesByLabel.put(node.label(), propertyTypes);
        });
        Set<RelationshipType> validRelTypes = new LinkedHashSet<>();
        ManufacturingOntology.ONTOLOGY.relationshipTypes().forEach(relationship -> validRelTypes.add(relationship.relType()));

        Set<String> nodeIds = new LinkedHashSet<>();
        for (int index = 0; index < mapping.nodes().size(); index++) {
            NodeMappingV2Dto node = mapping.nodes().get(index);
            if (node.nodeId() == null || node.nodeId().isBlank()) {
                errors.add(new ValidationIssue(
                        "MISSING_NODE_ID",
                        "error",
                        "node_id는 필수입니다",
                        "nodes[" + index + "].node_id",
                        null
                ));
                continue;
            }

            if (!nodeIds.add(node.nodeId())) {
                errors.add(new ValidationIssue(
                        "DUPLICATE_NODE_ID",
                        "error",
                        "중복된 node_id입니다: " + node.nodeId(),
                        "nodes[" + index + "].node_id",
                        null
                ));
            }

            Map<String, PropertyDataType> propertyTypes = nodePropertyTypesByLabel.getOrDefault(node.label(), Map.of());
            validateNodeColumns(index, node, propertyTypes, headerSet, sampleRows, errors, warnings);
        }

        for (int index = 0; index < mapping.relations().size(); index++) {
            RelationMappingV2Dto relation = mapping.relations().get(index);
            if (relation.relType() == null || !validRelTypes.contains(relation.relType())) {
                errors.add(new ValidationIssue(
                        "INVALID_REL_TYPE",
                        "error",
                        "허용되지 않은 관계 타입입니다: " + relation.relType(),
                        "relations[" + index + "].rel_type",
                        null
                ));
                continue;
            }

            if (!nodeIds.contains(relation.fromNodeId())) {
                errors.add(new ValidationIssue(
                        "UNKNOWN_FROM_NODE",
                        "error",
                        "정의되지 않은 from_node_id입니다: " + relation.fromNodeId(),
                        "relations[" + index + "].from_node_id",
                        null
                ));
            }
            if (!nodeIds.contains(relation.toNodeId())) {
                errors.add(new ValidationIssue(
                        "UNKNOWN_TO_NODE",
                        "error",
                        "정의되지 않은 to_node_id입니다: " + relation.toNodeId(),
                        "relations[" + index + "].to_node_id",
                        null
                ));
            }

            validateRelationColumns(index, relation, headerSet, sampleRows, errors, warnings);
        }

        int disabledCount = 0;
        List<String> usedColumns = mapping.requiredColumns();
        for (String header : headers) {
            if (!usedColumns.contains(header)) {
                disabledCount++;
            }
        }

        return new ValidationResult(
                errors,
                warnings,
                new ValidationImpactSummary(disabledCount)
        );
    }

    private void validateNodeColumns(
            int index,
            NodeMappingV2Dto node,
            Map<String, PropertyDataType> propertyTypes,
            Set<String> headerSet,
            List<Map<String, Object>> sampleRows,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        for (Map.Entry<String, String> property : node.propertyColumns().entrySet()) {
            String sourceColumn = property.getValue();
            String path = "nodes[" + index + "].property_columns." + property.getKey();
            if (sourceColumn == null || !headerSet.contains(sourceColumn)) {
                errors.add(new ValidationIssue(
                        "MISSING_SOURCE_COLUMN",
                        "error",
                        "컬럼 '" + sourceColumn + "'을(를) 파일에서 찾을 수 없습니다",
                        path,
                        "missing_source_column"
                ));
                continue;
            }

            PropertyDataType dataType = propertyTypes.getOrDefault(property.getKey(), PropertyDataType.STRING);
            if (isNumericType(dataType) && hasNonNumericSample(sampleRows, sourceColumn)) {
                warnings.add(new ValidationIssue(
                        "NUMERIC_PARSE_WARNING",
                        "warning",
                        "컬럼 '" + sourceColumn + "'에 숫자로 해석하기 어려운 값이 있습니다",
                        path,
                        null
                ));
            }
        }

        for (int extIndex = 0; extIndex < node.extendedProperties().size(); extIndex++) {
            ExtendedPropertyMappingV2Dto property = node.extendedProperties().get(extIndex);
            String path = "nodes[" + index + "].extended_properties[" + extIndex + "].source_column";
            validateExtendedProperty(path, property, headerSet, sampleRows, errors, warnings);
        }
    }

    private void validateRelationColumns(
            int index,
            RelationMappingV2Dto relation,
            Set<String> headerSet,
            List<Map<String, Object>> sampleRows,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        for (Map.Entry<String, String> property : relation.propertyColumns().entrySet()) {
            String sourceColumn = property.getValue();
            String path = "relations[" + index + "].property_columns." + property.getKey();
            if (sourceColumn == null || !headerSet.contains(sourceColumn)) {
                errors.add(new ValidationIssue(
                        "MISSING_SOURCE_COLUMN",
                        "error",
                        "관계 컬럼 '" + sourceColumn + "'을(를) 파일에서 찾을 수 없습니다",
                        path,
                        "missing_source_column"
                ));
                continue;
            }

            PropertyDataType dataType = relation.propertyColumnTypes().getOrDefault(property.getKey(), PropertyDataType.STRING);
            if (isNumericType(dataType) && hasNonNumericSample(sampleRows, sourceColumn)) {
                warnings.add(new ValidationIssue(
                        "NUMERIC_PARSE_WARNING",
                        "warning",
                        "관계 컬럼 '" + sourceColumn + "'에 숫자로 해석하기 어려운 값이 있습니다",
                        path,
                        null
                ));
            }
        }

        for (int extIndex = 0; extIndex < relation.extendedProperties().size(); extIndex++) {
            ExtendedPropertyMappingV2Dto property = relation.extendedProperties().get(extIndex);
            String path = "relations[" + index + "].extended_properties[" + extIndex + "].source_column";
            validateExtendedProperty(path, property, headerSet, sampleRows, errors, warnings);
        }
    }

    private void validateExtendedProperty(
            String path,
            ExtendedPropertyMappingV2Dto property,
            Set<String> headerSet,
            List<Map<String, Object>> sampleRows,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        if (property.sourceColumn() == null || !headerSet.contains(property.sourceColumn())) {
            errors.add(new ValidationIssue(
                    "MISSING_SOURCE_COLUMN",
                    "error",
                    "컬럼 '" + property.sourceColumn() + "'을(를) 파일에서 찾을 수 없습니다",
                    path,
                    "missing_source_column"
            ));
            return;
        }

        if (isNumericType(property.dataType()) && hasNonNumericSample(sampleRows, property.sourceColumn())) {
            warnings.add(new ValidationIssue(
                    "NUMERIC_PARSE_WARNING",
                    "warning",
                    "컬럼 '" + property.sourceColumn() + "'에 숫자로 해석하기 어려운 값이 있습니다",
                    path,
                    null
            ));
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

    public record ValidationIssue(
            String code,
            String severity,
            String message,
            String path,
            String dismissedReason
    ) {
    }

    public record ValidationImpactSummary(
            int disabledColumnCount
    ) {
    }

    public record ValidationResult(
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings,
            ValidationImpactSummary impactSummary
    ) {
    }
}
