package com.fabbitinc.server.application.mapping.query.result;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;
import java.util.Map;

public record RelationMappingResult(
        RelationshipType relType,
        String targetLabel,
        Map<String, String> nodeColumns,
        Map<String, String> relColumns,
        Map<String, PropertyDataType> relColumnTypes,
        Integer confidence,
        String reason
) {
    public RelationMappingResult {
        nodeColumns = nodeColumns == null ? Map.of() : Map.copyOf(nodeColumns);
        relColumns = relColumns == null ? Map.of() : Map.copyOf(relColumns);
        relColumnTypes = relColumnTypes == null ? Map.of() : Map.copyOf(relColumnTypes);
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
    }
}
