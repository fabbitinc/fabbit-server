package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.mapping.dto.common.PropertyMappingDto;
import com.fabbitinc.server.application.mapping.dto.common.RelationMappingDto;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class MappingNormalizationSupport {

    public MappingResultDto normalize(MappingResultDto raw) {
        Set<String> validLabels = new LinkedHashSet<>();
        Set<RelationshipType> validRelTypes = new LinkedHashSet<>();
        Map<String, Set<String>> mergeKeysByLabel = new LinkedHashMap<>();
        Map<RelationshipType, Map<String, PropertyDataType>> relPropertyTypes = new LinkedHashMap<>();

        for (ManufacturingOntology.NodeLabelDef nodeLabel : ManufacturingOntology.ONTOLOGY.nodeLabels()) {
            validLabels.add(nodeLabel.label());
            mergeKeysByLabel.put(nodeLabel.label(), new LinkedHashSet<>(nodeLabel.mergeKeys()));
        }
        for (ManufacturingOntology.RelationshipTypeDef relationshipType : ManufacturingOntology.ONTOLOGY.relationshipTypes()) {
            validRelTypes.add(relationshipType.relType());
            Map<String, PropertyDataType> propertyTypeMap = new LinkedHashMap<>();
            relationshipType.properties().forEach(property -> propertyTypeMap.put(property.name(), property.dataType()));
            relPropertyTypes.put(relationshipType.relType(), propertyTypeMap);
        }

        Set<String> partProperties = new LinkedHashSet<>();
        ManufacturingOntology.ONTOLOGY.nodeLabels().stream()
                .filter(node -> "Part".equals(node.label()))
                .findFirst()
                .ifPresent(node -> node.properties().forEach(property -> partProperties.add(property.name())));

        List<PropertyMappingDto> normalizedProperties = normalizeProperties(raw.propertyMappings(), partProperties);
        List<RelationMappingDto> normalizedRelations = normalizeRelations(
                raw.relationMappings(),
                validRelTypes,
                validLabels,
                mergeKeysByLabel,
                relPropertyTypes
        );

        return new MappingResultDto(normalizedProperties, normalizedRelations);
    }

    private List<PropertyMappingDto> normalizeProperties(
            List<PropertyMappingDto> properties,
            Set<String> partProperties
    ) {
        List<PropertyMappingDto> verified = new ArrayList<>();

        for (PropertyMappingDto property : properties) {
            String target = property.targetProperty() == null ? "" : property.targetProperty();
            if (target.startsWith("_ext_")) {
                String normalized = ExtendedPropertySupport.normalizeExtendedProperty(target);
                verified.add(new PropertyMappingDto(
                        property.sourceColumn(),
                        normalized,
                        property.suggestedExtendedProperty(),
                        property.dataType(),
                        property.confidence(),
                        property.reason(),
                        true
                ));
                continue;
            }

            if (partProperties.contains(target)) {
                verified.add(new PropertyMappingDto(
                        property.sourceColumn(),
                        target,
                        property.suggestedExtendedProperty(),
                        property.dataType(),
                        property.confidence(),
                        property.reason(),
                        false
                ));
                continue;
            }

            verified.add(new PropertyMappingDto(
                    property.sourceColumn(),
                    ExtendedPropertySupport.normalizeExtendedProperty("_ext_" + target),
                    property.suggestedExtendedProperty(),
                    property.dataType(),
                    property.confidence(),
                    property.reason(),
                    true
            ));
        }

        List<PropertyMappingDto> deduplicated = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (PropertyMappingDto property : verified) {
            String key = (property.sourceColumn() == null ? "" : property.sourceColumn())
                    + "::"
                    + property.targetProperty();
            if (seen.add(key)) {
                deduplicated.add(property);
            }
        }
        return deduplicated;
    }

    private List<RelationMappingDto> normalizeRelations(
            List<RelationMappingDto> relations,
            Set<RelationshipType> validRelTypes,
            Set<String> validLabels,
            Map<String, Set<String>> mergeKeysByLabel,
            Map<RelationshipType, Map<String, PropertyDataType>> relPropertyTypes
    ) {
        List<RelationMappingDto> verified = new ArrayList<>();

        for (RelationMappingDto relation : relations) {
            if (relation.relType() == null || relation.targetLabel() == null) {
                continue;
            }
            if (!validRelTypes.contains(relation.relType())) {
                continue;
            }
            if (!validLabels.contains(relation.targetLabel())) {
                continue;
            }

            Map<String, String> nodeColumns = relation.nodeColumns();
            Map<String, String> relColumns = relation.relColumns();
            boolean rootless = nodeColumns.isEmpty() && !relColumns.isEmpty();

            RelationMappingDto fixedTypes = fixRelationColumnTypes(relation, relPropertyTypes);
            if (rootless) {
                verified.add(fixedTypes);
                continue;
            }

            if (nodeColumns.isEmpty()) {
                continue;
            }

            Set<String> mergeKeys = mergeKeysByLabel.getOrDefault(relation.targetLabel(), Set.of());
            boolean hasMergeKey = mergeKeys.stream().anyMatch(nodeColumns::containsKey);
            if (!hasMergeKey) {
                continue;
            }

            verified.add(fixedTypes);
        }

        return verified;
    }

    private RelationMappingDto fixRelationColumnTypes(
            RelationMappingDto relation,
            Map<RelationshipType, Map<String, PropertyDataType>> relPropertyTypes
    ) {
        if (relation.relColumns().isEmpty()) {
            return relation;
        }

        Map<String, PropertyDataType> ontologyTypes = relPropertyTypes.getOrDefault(relation.relType(), Map.of());
        Map<String, PropertyDataType> fixed = new LinkedHashMap<>(relation.relColumnTypes());

        for (String relProperty : relation.relColumns().keySet()) {
            if (!fixed.containsKey(relProperty) && ontologyTypes.containsKey(relProperty)) {
                fixed.put(relProperty, ontologyTypes.get(relProperty));
            }
        }

        return new RelationMappingDto(
                relation.relType(),
                relation.targetLabel(),
                relation.nodeColumns(),
                relation.relColumns(),
                fixed,
                relation.confidence(),
                relation.reason()
        );
    }
}
