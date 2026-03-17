package com.fabbitinc.server.presentation.workitem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "타임라인 참조 항목")
public record TimelineRefResponse(
        @Schema(description = "참조 식별자")
        String id,
        @Schema(description = "참조 타입", example = "issue")
        String type,
        @Schema(description = "표시 라벨", example = "#12 브래킷 깨짐")
        String label,
        @Schema(description = "보조 메타데이터")
        JsonNode meta
) {
}
