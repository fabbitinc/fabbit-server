package com.fabbitinc.server.presentation.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.PropertyDataType;
import com.fabbitinc.server.application.ontology.support.RelationshipType;

import java.util.List;

public record OntologySchemaResponse(
        String name,
        String description,
        List<NodeLabelSchemaResponse> nodeLabels,
        List<RelationshipTypeSchemaResponse> relationshipTypes
) {
    public record NodeLabelSchemaResponse(
            String label,
            String description,
            List<PropertySchemaResponse> properties,
            List<String> mergeKeys
    ) {
    }

    public record RelationshipTypeSchemaResponse(
            RelationshipType relType,
            String description,
            String fromLabel,
            String toLabel,
            List<PropertySchemaResponse> properties
    ) {
    }

    public record PropertySchemaResponse(
            String name,
            String description,
            PropertyDataType dataType,
            boolean required,
            boolean isMergeKey
    ) {
    }
}
