package com.fabbitinc.server.presentation.settings.response;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "설정 부품 워크플로 정책 응답 DTO")
public record SettingsPartWorkflowPolicyResponse(
        @Schema(description = "부품 리비전 워크플로 모드", example = "DIRECT")
        PartRevisionWorkflowMode mode
) {
}
