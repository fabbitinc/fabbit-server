package com.fabbitinc.server.presentation.settings.response;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "설정 응답 DTO")
public record SettingsResponse(
        @Schema(description = "부품 리비전 워크플로 모드", example = "DIRECT")
        PartRevisionWorkflowMode partWorkflowMode
) {
}
