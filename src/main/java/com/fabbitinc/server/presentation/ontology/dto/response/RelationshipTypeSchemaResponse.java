package com.fabbitinc.server.presentation.ontology.dto.response;

import com.fabbitinc.server.application.ontology.support.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
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
