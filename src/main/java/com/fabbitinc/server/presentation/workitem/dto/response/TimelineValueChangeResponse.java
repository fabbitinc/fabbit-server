package com.fabbitinc.server.presentation.workitem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

@Schema(description = "타임라인 값 변경 항목")
public record TimelineValueChangeResponse(
        @Schema(description = "이전 값")
        JsonNode oldValue,
        @Schema(description = "이후 값")
        JsonNode newValue
) {
}
