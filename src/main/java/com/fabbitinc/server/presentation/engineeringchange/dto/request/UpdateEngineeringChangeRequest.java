package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import tools.jackson.databind.JsonNode;

@Schema(description = "변경관리 수정 요청")
public record UpdateEngineeringChangeRequest(
        @Size(min = 1, max = 500) @Schema(description = "변경관리 제목")
        String title,
        @Schema(description = "변경관리 본문(TipTap JSON)")
        JsonNode body,
        @Schema(description = "변경관리 단계 목록")
        @Valid List<EngineeringChangeStepRequest> steps
) {
    public UpdateEngineeringChangeRequest {
        steps = steps == null ? null : List.copyOf(steps);
    }
}
