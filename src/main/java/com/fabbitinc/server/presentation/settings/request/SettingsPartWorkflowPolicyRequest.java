package com.fabbitinc.server.presentation.settings.request;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "설정 부품 워크플로 정책 변경 요청")
public record SettingsPartWorkflowPolicyRequest(
        @Schema(
                description = "부품 리비전 워크플로 모드",
                example = "DIRECT",
                allowableValues = {"DIRECT", "ENGINEERING_CHANGE_REQUIRED"}
        )
        @NotNull(message = "mode는 필수입니다") PartRevisionWorkflowMode mode
) {
}
