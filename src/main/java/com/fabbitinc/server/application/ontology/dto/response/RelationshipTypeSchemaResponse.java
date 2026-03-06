package com.fabbitinc.server.application.ontology.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.application.ontology.support.RelationshipType;

import java.util.List;

@Schema(description = "응답 DTO")
public record RelationshipTypeSchemaResponse(
        RelationshipType relType,
        String description,
        String fromLabel,
        String toLabel,
        List<PropertySchemaResponse> properties
) {
}
