package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "워크플로우 템플릿 응답")
public record WorkflowTemplateResponse(
        @Schema(description = "템플릿 ID")
        UUID id,
        @Schema(description = "템플릿 이름")
        String name,
        @Schema(description = "템플릿 설명")
        String description,
        @Schema(description = "단계 목록")
        List<WorkflowTemplateStageResponse> stages,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
