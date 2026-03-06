package com.fabbitinc.server.application.ontology.query.result;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;

import java.util.List;

public record OntologySchemaResult(
        String name,
        String description,
        List<NodeLabelResult> nodeLabels,
        List<RelationshipTypeResult> relationshipTypes
) {
    public record NodeLabelResult(
            String label,
            String description,
            List<PropertyResult> properties,
            List<String> mergeKeys
    ) {
    }

    public record RelationshipTypeResult(
            RelationshipType relType,
            String description,
            String fromLabel,
            String toLabel,
            List<PropertyResult> properties
    ) {
    }

    public record PropertyResult(
            String name,
            String description,
            PropertyDataType dataType,
            boolean required,
            boolean isMergeKey
    ) {
    }
}
