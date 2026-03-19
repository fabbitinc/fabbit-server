package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.mapping.support.ExtendedPropertySupport;
import com.fabbitinc.server.application.mapping.model.ExtendedPropertyMappingDto;
import com.fabbitinc.server.application.mapping.model.MappingResultDto;
import com.fabbitinc.server.application.mapping.model.NodeMappingDto;
import com.fabbitinc.server.application.mapping.model.RelationMappingDto;
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
public class MappingNormalizationSupport {

    public MappingResultDto normalize(MappingResultDto raw) {
        Map<String, ManufacturingOntology.NodeLabelDef> nodeDefsByLabel = new LinkedHashMap<>();
        Map<RelationshipType, ManufacturingOntology.RelationshipTypeDef> relationDefsByType = new LinkedHashMap<>();

        ManufacturingOntology.ONTOLOGY.nodeLabels().forEach(node -> nodeDefsByLabel.put(node.label(), node));
        ManufacturingOntology.ONTOLOGY.relationshipTypes().forEach(relation -> relationDefsByType.put(relation.relType(), relation));

        List<NodeMappingDto> normalizedNodes = normalizeNodes(raw.nodes(), nodeDefsByLabel);
        Map<String, NodeMappingDto> nodesById = new LinkedHashMap<>();
        normalizedNodes.forEach(node -> nodesById.put(node.nodeId(), node));

        List<RelationMappingDto> normalizedRelations = normalizeRelations(raw.relations(), nodesById, relationDefsByType);
        return new MappingResultDto(normalizedNodes, normalizedRelations);
    }

    private List<NodeMappingDto> normalizeNodes(
            List<NodeMappingDto> nodes,
            Map<String, ManufacturingOntology.NodeLabelDef> nodeDefsByLabel
    ) {
        List<NodeMappingDto> normalized = new ArrayList<>();
        Set<String> seenNodeIds = new LinkedHashSet<>();

        for (NodeMappingDto node : nodes) {
            String nodeId = trimToNull(node.nodeId());
            String label = trimToNull(node.label());
            if (nodeId == null || label == null || !seenNodeIds.add(nodeId)) {
                continue;
            }

            ManufacturingOntology.NodeLabelDef nodeDef = nodeDefsByLabel.get(label);
            if (nodeDef == null) {
                continue;
            }

            Set<String> validProperties = new LinkedHashSet<>();
            Map<String, PropertyDataType> propertyTypes = new LinkedHashMap<>();
            nodeDef.properties().forEach(property -> {
                validProperties.add(property.name());
                propertyTypes.put(property.name(), property.dataType());
            });

            Map<String, String> propertyColumns = new LinkedHashMap<>();
            List<ExtendedPropertyMappingDto> promoted = new ArrayList<>();
            for (Map.Entry<String, String> entry : node.propertyColumns().entrySet()) {
                String propertyName = trimToNull(entry.getKey());
                if (propertyName == null) {
                    continue;
                }
                if (validProperties.contains(propertyName) && !ExtendedPropertySupport.isExtendedProperty(propertyName)) {
                    propertyColumns.putIfAbsent(propertyName, entry.getValue());
                    continue;
                }

                promoted.add(new ExtendedPropertyMappingDto(
                        entry.getValue(),
                        propertyName,
                        propertyTypes.getOrDefault(propertyName, PropertyDataType.STRING)
                ));
            }

            normalized.add(new NodeMappingDto(
                    nodeId,
                    label,
                    propertyColumns,
                    normalizeExtendedProperties(promoted, node.extendedProperties()),
                    node.confidence(),
                    node.reason()
            ));
        }

        return normalized;
    }

    private List<RelationMappingDto> normalizeRelations(
            List<RelationMappingDto> relations,
            Map<String, NodeMappingDto> nodesById,
            Map<RelationshipType, ManufacturingOntology.RelationshipTypeDef> relationDefsByType
    ) {
        List<RelationMappingDto> normalized = new ArrayList<>();

        for (RelationMappingDto relation : relations) {
            String fromNodeId = trimToNull(relation.fromNodeId());
            String toNodeId = trimToNull(relation.toNodeId());
            if (fromNodeId == null || toNodeId == null || relation.relType() == null) {
                continue;
            }

            NodeMappingDto fromNode = nodesById.get(fromNodeId);
            NodeMappingDto toNode = nodesById.get(toNodeId);
            ManufacturingOntology.RelationshipTypeDef relationDef = relationDefsByType.get(relation.relType());
            if (fromNode == null || toNode == null || relationDef == null) {
                continue;
            }

            if (!relationDef.fromLabel().equals(fromNode.label()) || !relationDef.toLabel().equals(toNode.label())) {
                continue;
            }

            Map<String, PropertyDataType> validProperties = new LinkedHashMap<>();
            relationDef.properties().forEach(property -> validProperties.put(property.name(), property.dataType()));

            Map<String, String> propertyColumns = new LinkedHashMap<>();
            Map<String, PropertyDataType> propertyColumnTypes = new LinkedHashMap<>();
            List<ExtendedPropertyMappingDto> promoted = new ArrayList<>();
            for (Map.Entry<String, String> entry : relation.propertyColumns().entrySet()) {
                String propertyName = trimToNull(entry.getKey());
                if (propertyName == null) {
                    continue;
                }
                if (validProperties.containsKey(propertyName) && !ExtendedPropertySupport.isExtendedProperty(propertyName)) {
                    propertyColumns.putIfAbsent(propertyName, entry.getValue());
                    propertyColumnTypes.putIfAbsent(
                            propertyName,
                            relation.propertyColumnTypes().getOrDefault(propertyName, validProperties.get(propertyName))
                    );
                    continue;
                }

                promoted.add(new ExtendedPropertyMappingDto(
                        entry.getValue(),
                        propertyName,
                        relation.propertyColumnTypes().getOrDefault(propertyName, PropertyDataType.STRING)
                ));
            }

            normalized.add(new RelationMappingDto(
                    fromNodeId,
                    relation.relType(),
                    toNodeId,
                    propertyColumns,
                    propertyColumnTypes,
                    normalizeExtendedProperties(promoted, relation.extendedProperties()),
                    relation.confidence(),
                    relation.reason()
            ));
        }

        return normalized;
    }

    private List<ExtendedPropertyMappingDto> normalizeExtendedProperties(
            List<ExtendedPropertyMappingDto> promoted,
            List<ExtendedPropertyMappingDto> explicit
    ) {
        List<ExtendedPropertyMappingDto> normalized = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<ExtendedPropertyMappingDto> combined = new ArrayList<>(promoted);
        combined.addAll(explicit);

        for (ExtendedPropertyMappingDto property : combined) {
            String sourceColumn = trimToNull(property.sourceColumn());
            if (sourceColumn == null) {
                continue;
            }

            String generatedKey = resolveGeneratedKey(property.generatedKey(), sourceColumn);
            String dedupeKey = sourceColumn + "::" + generatedKey;
            if (!seen.add(dedupeKey)) {
                continue;
            }

            normalized.add(new ExtendedPropertyMappingDto(
                    sourceColumn,
                    generatedKey,
                    property.dataType()
            ));
        }

        return normalized;
    }

    private String resolveGeneratedKey(String rawKey, String sourceColumn) {
        String candidate = trimToNull(rawKey);
        if (candidate == null) {
            candidate = sourceColumn;
        }
        if (!ExtendedPropertySupport.isExtendedProperty(candidate)) {
            candidate = "_ext_" + candidate;
        }
        return ExtendedPropertySupport.normalizeExtendedProperty(candidate);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
