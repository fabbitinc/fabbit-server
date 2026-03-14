package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 리비전 워크플로 정책 응답")
public record PartWorkflowPolicyResponse(
        @Schema(description = "리비전 워크플로 모드", example = "DIRECT")
        PartRevisionWorkflowMode mode
) {
}
