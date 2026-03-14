package com.fabbitinc.server.application.part.dto.request;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "부품 리비전 워크플로 정책 변경 요청")
public record PartWorkflowPolicyRequest(
        @Schema(
                description = "리비전 워크플로 모드",
                example = "DIRECT",
                allowableValues = {"DIRECT", "CHANGE_REQUEST_REQUIRED"}
        )
        @NotNull(message = "mode는 필수입니다")
        PartRevisionWorkflowMode mode
) {
}
