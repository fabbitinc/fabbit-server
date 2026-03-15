package com.fabbitinc.server.presentation.ontology.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "응답 DTO")
public record NodeSearchItemResponse(
        String value,
        String label
) {
}
